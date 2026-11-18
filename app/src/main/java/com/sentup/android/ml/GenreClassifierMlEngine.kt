package com.sentup.android.ml

import android.content.Context
import com.sentup.android.data.CustomFolderTree
import com.sentup.android.data.MusicFolder
import com.sentup.android.data.Song
import com.sentup.android.data.VpnResilientNetwork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.exp
import kotlin.math.ln

/**
 * Hybrid Online + On-Device Song & Artist Genre Classification ML Engine.
 *
 * Combines:
 * 1. Curated Turkish + Global artist identity priors (collab-aware splitting),
 * 2. Verified external genre tag fusion (iTunes TR-first + US fallback primaryGenreName, cached 30 days),
 * 3. Multi-song voting + diacritic-folded word/phrase log-odds scoring over artist/title/album,
 * 4. Acoustic duration priors,
 * 5. Folder co-occurrence k-NN label propagation across artists in the user's folders.
 */
object GenreClassifierMlEngine {

    enum class MusicGenre(val displayNameTr: String) {
        AnadoluRock("Anadolu Rock"),
        RockMetal("Rock & Metal"),
        Pop("Pop"),
        RapHipHop("Rap & Hip-Hop"),
        ElectronicDance("Elektronik & Dance"),
        LofiChill("Lo-Fi & Chill"),
        AcousticIndie("Akustik & Indie"),
        ArabeskFantezi("Arabesk & Fantezi"),
        JazzBlues("Jazz & Blues"),
        ClassicalInstrumental("Klasik & Enstrümantal")
    }

    data class GenrePrediction(
        val genre: MusicGenre,
        val confidence: Float,
        val matchedSignals: List<String>,
        val isVerifiedTag: Boolean = false
    )

    data class ArtistGenreCluster(
        val genre: MusicGenre,
        val artists: List<String>,
        val totalSongs: Int,
        val averageConfidence: Float
    )

    data class FolderGenreBreakdownItem(
        val genre: MusicGenre,
        val count: Int,
        val percentage: Int
    )

    data class FolderGenreProfile(
        val dominantGenre: MusicGenre?,
        val dominantConfidence: Float,
        val breakdown: List<FolderGenreBreakdownItem>,
        val totalItems: Int,
        val vibeSummary: String
    )

    data class GenreSubfolderSplit(
        val genre: MusicGenre,
        val folderName: String,
        val artists: List<String>,
        val albums: List<String>,
        val songIds: List<String>
    )

    private data class GenrePrototype(
        val genre: MusicGenre,
        val strongArtists: Set<String>,
        val tokenWeights: Map<String, Double>,
        val phraseWeights: Map<String, Double> = emptyMap(),
        val preferredDurationRangeMs: LongRange? = null
    )

    private const val PREFS_GENRE_CACHE = "genre_ml_cache"
    private const val CACHE_KEY_PREFIX = "ml2:"
    private const val OVERRIDE_KEY_PREFIX = "override:"
    private const val CACHE_MISS_MARKER = "__MISS__"
    private const val CACHE_TTL_HIT_MS = 30L * 24L * 60L * 60L * 1000L
    private const val CACHE_TTL_MISS_MS = 24L * 60L * 60L * 1000L
    private val trLocale = Locale.forLanguageTag("tr-TR")
    private val collabSplitRegex = Regex("""\s*(?:,|&|\bx\b|\bft\.?\b|\bfeat\.?\b|\bwith\b|\bve\b)\s*""", RegexOption.IGNORE_CASE)
    private val tokenSplitRegex = Regex("[^a-z0-9]+")

    private val httpClient by lazy {
        VpnResilientNetwork.configureForVpn(OkHttpClient.Builder())
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val prototypes: List<GenrePrototype> = listOf(
        GenrePrototype(
            genre = MusicGenre.AnadoluRock,
            strongArtists = setOf(
                "emre aydin", "demir demirkan", "sebnem ferah", "ogun sanlisoy",
                "mor ve otesi", "cilekes", "pilli bebek", "redd", "duman", "teoman",
                "manga", "kurban", "hayko cepkin", "baris manco", "cem karaca",
                "erkin koray", "haluk levent", "athena", "yuksek sadakat", "zakkum",
                "gece yolculari", "pentagram", "adamlar", "yuzyuzeyken konusuruz",
                "buyuk ev ablukada", "mavi sakal", "bulutsuzluk ozlemi", "vega",
                "gripin", "model", "fatma turgut", "seksendort", "kargo", "feridun duzagac",
                "tuna kiremitci", "pinhani", "yavuz cetin", "mogollar", "mfo",
                "altin gun", "mavi gri", "perdenin ardindakiler", "ikilem", "yasli amca",
                "son feci bisiklet", "ucnoktabir", "ilhan irem", "edip akbayram",
                "fikret kizilok", "bulent ortacgil", "evdeki saat", "yok oyle kararli seyler",
                "kurtalan ekspres", "uc hur el", "3 hur el", "hardal", "flort",
                "badem", "ayna", "destan", "ozlem tekin", "baris akarsu", "kazim koyuncu"
            ),
            tokenWeights = mapOf(
                "anadolu" to 3.2, "rock" to 1.6, "gitar" to 1.4, "konser" to 0.9,
                "senfonik" to 1.1, "yalniz" to 0.6, "ruzgar" to 0.6, "firtina" to 0.6
            ),
            phraseWeights = mapOf(
                "anadolu rock" to 4.2, "turkce rock" to 4.0, "turkish rock" to 4.2,
                "anatolian rock" to 4.4
            )
        ),
        GenrePrototype(
            genre = MusicGenre.RockMetal,
            strongArtists = setOf(
                "metallica", "nirvana", "linkin park", "queen", "pink floyd", "ac dc", "ac/dc",
                "iron maiden", "megadeth", "system of a down", "rammstein", "slipknot",
                "muse", "radiohead", "arctic monkeys", "foo fighters", "red hot chili peppers",
                "green day", "evanescence", "korn", "deftones", "tool", "ozzy osbourne",
                "black sabbath", "led zeppelin", "deep purple", "scorpions", "guns n roses",
                "pearl jam", "soundgarden", "alice in chains", "slayer", "pantera",
                "disturbed", "avenged sevenfold", "bring me the horizon", "the strokes",
                "opeth", "gojira", "dream theater", "nightwish", "epica", "within temptation",
                "blind guardian", "sabaton", "powerwolf", "arch enemy", "amon amarth",
                "judas priest", "motorhead", "ramones", "the clash", "the cure",
                "depeche mode", "joy division", "the smiths", "oasis", "blur", "gorillaz",
                "the beatles", "the rolling stones", "the doors", "jimi hendrix", "david bowie",
                "audioslave", "rage against the machine", "three days grace", "breaking benjamin",
                "skillet", "my chemical romance", "fall out boy", "paramore", "twenty one pilots",
                "coldplay", "manowar", "dio", "kiss", "aerosmith", "bon jovi", "def leppard"
            ),
            tokenWeights = mapOf(
                "metal" to 3.4, "rock" to 2.5, "punk" to 2.6, "grunge" to 2.8,
                "riff" to 2.1, "heavy" to 1.9, "thrash" to 2.8, "hardcore" to 2.3,
                "deathcore" to 2.8, "metalcore" to 2.9, "alternative" to 1.4, "unplugged" to 1.1
            ),
            phraseWeights = mapOf(
                "hard rock" to 3.6, "heavy metal" to 3.8, "alt rock" to 3.2,
                "nu metal" to 3.6, "progressive rock" to 3.4
            ),
            preferredDurationRangeMs = 215_000L..440_000L
        ),
        GenrePrototype(
            genre = MusicGenre.Pop,
            strongArtists = setOf(
                "tarkan", "sezen aksu", "sertab erener", "kenan dogulu", "hande yener",
                "demet akalin", "gulsen", "hadise", "murat boz", "aleyna tilki", "edis",
                "zeynep bastik", "sila", "yalin", "gokhan turkmen", "mabel matiz", "simge",
                "melek mosso", "mustafa sandal", "serdar ortac", "nil karaibrahimgil",
                "semicenk", "emir can igrek", "reynmen", "gunes", "alizade", "buray",
                "irem derici", "ilyas yalcintas", "bilal sonses", "mustafa ceceli",
                "oguzhan koc", "ozan dogulu", "tugce kandemir", "derya ulug", "ziynet sali",
                "bengu", "ajda pekkan", "nukhet duru", "candan ercetin", "levent yuksel",
                "emre altug", "gokhan ozen", "gokhan tepe", "ferhat gocer", "murat dalkilic",
                "berkay", "cem belevi", "tan tasci",
                "taylor swift", "dua lipa", "the weeknd", "ariana grande", "billie eilish",
                "ed sheeran", "justin bieber", "bruno mars", "katy perry", "lady gaga",
                "olivia rodrigo", "sabrina carpenter", "shawn mendes", "harry styles",
                "beyonce", "rihanna", "adele", "charlie puth", "selena gomez", "miley cyrus",
                "sia", "imagine dragons", "maroon 5", "onerepublic", "michael jackson",
                "madonna", "bts", "blackpink", "stray kids", "newjeans", "doja cat",
                "sza", "bad bunny", "karol g", "shakira", "rosalia", "the neighbourhood"
            ),
            tokenWeights = mapOf(
                "pop" to 2.9, "single" to 0.8, "hit" to 0.9, "dance" to 1.0, "kpop" to 3.2
            ),
            phraseWeights = mapOf(
                "turkish pop" to 3.8, "turkce pop" to 3.8, "synth pop" to 3.0, "dance pop" to 2.8
            ),
            preferredDurationRangeMs = 150_000L..235_000L
        ),
        GenrePrototype(
            genre = MusicGenre.RapHipHop,
            strongArtists = setOf(
                "ceza", "sagopa kajmer", "ezhel", "ben fero", "sehinsah", "hidra",
                "stabil", "allame", "contra", "gazapizm", "norm ender", "uzi",
                "lvbel c5", "cakal", "motive", "reckol", "batuflex", "khontkar",
                "sansar salvo", "mode xl", "fuat", "selo", "ati242", "sefo", "blok3",
                "era7capone", "poizi", "killoki", "organize", "murda", "jeff redd",
                "cash flow", "eypio", "patron", "kayra", "farazi", "da poet", "no 1",
                "no1", "heijan", "muti", "burak king", "tankurt manas", "anil piyanci",
                "sehabe", "massaka", "defkhan", "tepki", "ayben", "killa hakan",
                "server uraz", "baneva", "deha inc", "swirf", "joker", "vio", "velet",
                "aspova", "rota", "ceg", "cegid", "saian", "karacali", "kamufle", "indigo",
                "eminem", "drake", "kendrick lamar", "travis scott", "kanye west",
                "2pac", "tupac", "snoop dogg", "50 cent", "post malone", "j cole",
                "metro boomin", "playboi carti", "future", "21 savage", "ice cube",
                "asap rocky", "tyler the creator", "nicki minaj", "cardi b", "juice wrld",
                "xxxtentacion", "lil uzi vert", "lil wayne", "jay z", "nas", "mf doom",
                "wu tang clan", "outkast", "central cee", "stormzy", "skepta", "nle choppa",
                "gunna", "young thug", "yeat", "ken carson", "pop smoke", "lil baby",
                "migos", "offset", "quavo"
            ),
            tokenWeights = mapOf(
                "rap" to 3.4, "trap" to 2.9, "drill" to 3.0, "freestyle" to 2.8,
                "beat" to 1.6, "diss" to 2.8, "cypher" to 3.0, "flow" to 1.9, "prod" to 1.3
            ),
            phraseWeights = mapOf(
                "hip hop" to 3.6, "hip-hop" to 3.6, "turkce rap" to 3.9, "boom bap" to 3.4
            ),
            preferredDurationRangeMs = 120_000L..215_000L
        ),
        GenrePrototype(
            genre = MusicGenre.ElectronicDance,
            strongArtists = setOf(
                "calvin harris", "david guetta", "martin garrix", "tiesto", "avicii",
                "daft punk", "skrillex", "deadmau5", "alan walker", "marshmello",
                "kygo", "mahmut orhan", "burak yeter", "beduk", "fred again",
                "peggy gou", "solomun", "boris brejcha", "charlotte de witte", "armin van buuren",
                "justice", "disclosure", "flume", "odesza", "illenium", "porter robinson",
                "madeon", "zedd", "diplo", "major lazer", "steve aoki", "hardwell",
                "afrojack", "alesso", "swedish house mafia", "eric prydz", "anyma",
                "tale of us", "amelie lens", "ben bohmer", "rufus du sol", "monolink",
                "kiasmos", "bonobo", "four tet", "bicep", "kordhell", "dvrst",
                "ghostface playa", "felix jaehn", "robin schulz", "lost frequencies", "mercan dede"
            ),
            tokenWeights = mapOf(
                "edm" to 3.4, "techno" to 3.4, "house" to 2.9, "trance" to 3.2,
                "dubstep" to 3.2, "club" to 2.0, "dj" to 2.2, "electro" to 2.9,
                "synth" to 1.9, "phonk" to 3.3, "remix" to 1.8, "rave" to 2.8,
                "nightcore" to 2.8, "sped" to 1.2
            ),
            phraseWeights = mapOf(
                "deep house" to 3.5, "tech house" to 3.5, "sped up" to 2.2, "dj set" to 3.0
            )
        ),
        GenrePrototype(
            genre = MusicGenre.LofiChill,
            strongArtists = setOf(
                "lofi girl", "chilledcow", "nujabes", "j dilla", "joji", "idealism",
                "kupla", "potsu", "eevee", "sleepermane", "hm surf", "rook1e",
                "saib", "tomppabeats", "casiio", "lindecis", "jinsang", "swum", "wun two"
            ),
            tokenWeights = mapOf(
                "lofi" to 3.6, "chill" to 2.7, "study" to 2.6, "sleep" to 2.3,
                "rain" to 1.5, "ambient" to 2.7, "relax" to 2.3, "slowed" to 2.6,
                "reverb" to 2.3, "chillout" to 3.0, "meditation" to 2.5
            ),
            phraseWeights = mapOf(
                "lo-fi" to 3.8, "lo fi" to 3.6, "slowed reverb" to 3.2, "chill beats" to 3.2
            )
        ),
        GenrePrototype(
            genre = MusicGenre.AcousticIndie,
            strongArtists = setOf(
                "kalben", "cem adrian", "can bonomo", "madrigal", "dktt",
                "dolu kadehi ters tut", "emre fel", "dedubluman", "mert demir",
                "sena sener", "melike sahin", "nova norda", "can ozan", "sedef sebuktekin",
                "halil sezai", "goksel", "sufle",
                "lana del rey", "hozier", "bon iver", "sufjan stevens", "vance joy",
                "cigarettes after sex", "phoebe bridgers", "tame impala", "mac demarco",
                "mitski", "clairo", "beabadoobee", "laufey", "noah kahan",
                "the lumineers", "fleet foxes", "mumford & sons", "lord huron",
                "elliott smith", "nick drake", "damien rice", "aurora",
                "daughter", "london grammar", "florence the machine", "the xx",
                "beach house", "benson boone", "lewis capaldi", "tom odell"
            ),
            tokenWeights = mapOf(
                "acoustic" to 3.0, "akustik" to 3.0, "indie" to 3.1, "folk" to 2.3,
                "session" to 1.6, "piano" to 1.2, "piyano" to 1.2, "singer" to 1.4
            ),
            phraseWeights = mapOf(
                "singer songwriter" to 3.2, "indie pop" to 2.8, "indie rock" to 2.6
            )
        ),
        GenrePrototype(
            genre = MusicGenre.ArabeskFantezi,
            strongArtists = setOf(
                "muslum gurses", "ibrahim tatlises", "ferdi tayfur", "orhan gencebay",
                "ahmet kaya", "yildiz tilbe", "ebru gundes", "hakan altun",
                "cengiz kurtoglu", "serkan kaya", "koray avci", "selda bagcan",
                "neset ertas", "asik veysel", "musa eroglu", "bergen", "azer bulbul",
                "ebru yasar", "gullu", "kibariye", "adnan senses", "emrah",
                "mahsun kirmizigul", "ozcan deniz", "hakki bulut", "gokhan guney",
                "kamuran akkor", "coskun sabah", "intizar", "latif dogan", "ismail yk",
                "sibel can", "bulent ersoy", "zeki muren", "muazzez ersoy",
                "funda arar", "volkan konak", "kivircik ali", "erkan ogur",
                "izzet yildizhan", "alisan", "huseyin kagit"
            ),
            tokenWeights = mapOf(
                "arabesk" to 3.8, "damar" to 2.9, "taverna" to 3.0, "fantezi" to 2.9,
                "baglama" to 2.5, "turku" to 3.0, "bozlak" to 3.1, "meyhane" to 2.6,
                "gazino" to 2.5, "uzun" to 0.7
            ),
            phraseWeights = mapOf(
                "uzun hava" to 3.2, "turk sanat" to 3.4, "halk muzigi" to 3.2
            )
        ),
        GenrePrototype(
            genre = MusicGenre.JazzBlues,
            strongArtists = setOf(
                "miles davis", "john coltrane", "louis armstrong", "ella fitzgerald",
                "frank sinatra", "nina simone", "b b king", "bb king", "amy winehouse",
                "norah jones", "chet baker", "bill evans", "herbie hancock",
                "kerem gorsev", "ilhan ersahin", "birsen tezer", "karsu",
                "charlie parker", "thelonious monk", "duke ellington", "billie holiday",
                "etta james", "ray charles", "stevie wonder", "muddy waters",
                "robert johnson", "eric clapton", "gary moore", "joe bonamassa",
                "john mayer", "snarky puppy", "kamasi washington", "robert glasper",
                "diana krall", "michael buble", "gregory porter", "jamie cullum", "frank ocean"
            ),
            tokenWeights = mapOf(
                "jazz" to 3.6, "blues" to 3.4, "swing" to 2.6, "sax" to 2.3,
                "saxophone" to 2.5, "bossa" to 2.8, "soul" to 2.2, "funk" to 2.0, "quartet" to 2.2
            ),
            phraseWeights = mapOf(
                "bossa nova" to 3.6, "smooth jazz" to 3.4, "neo soul" to 3.0
            )
        ),
        GenrePrototype(
            genre = MusicGenre.ClassicalInstrumental,
            strongArtists = setOf(
                "ludwig van beethoven", "beethoven", "mozart", "wolfgang amadeus mozart",
                "johann sebastian bach", "bach", "chopin", "vivaldi", "tchaikovsky",
                "hans zimmer", "ludovico einaudi", "yiruma", "fazil say", "evgeny grinko",
                "max richter", "olafur arnalds", "yann tiersen", "joe hisaishi",
                "ryuichi sakamoto", "enya", "ennio morricone", "john williams",
                "howard shore", "jeremy soule", "ramin djawadi", "nobuo uematsu",
                "vangelis", "jean michel jarre", "debussy", "claude debussy",
                "erik satie", "satie", "rachmaninoff", "franz liszt", "schubert",
                "brahms", "mahler", "paganini", "shostakovich", "prokofiev",
                "farid farjad", "can atilla"
            ),
            tokenWeights = mapOf(
                "symphony" to 3.4, "sonata" to 3.4, "concerto" to 3.4, "nocturne" to 3.2,
                "orchestra" to 3.0, "instrumental" to 2.9, "enstrumantal" to 2.9,
                "soundtrack" to 2.7, "ost" to 2.4, "classical" to 3.4, "klasik" to 3.0,
                "prelude" to 3.0, "etude" to 3.0, "waltz" to 2.6, "valse" to 2.6
            ),
            phraseWeights = mapOf(
                "film score" to 3.4, "piano solo" to 2.8, "string quartet" to 3.0
            ),
            preferredDurationRangeMs = 260_000L..900_000L
        )
    )

    private val protoIndexByGenre: Map<MusicGenre, Int> =
        prototypes.mapIndexed { idx, p -> p.genre to idx }.toMap()

    /**
     * Classifies a single [Song] using normalized word-boundary lexical scoring + artist profile + duration priors.
     */
    fun classifySong(song: Song): GenrePrediction {
        val artistNorm = foldAscii(song.artist)
        val combinedText = foldAscii("${song.artist} ${song.title} ${song.album}")
        return scoreEvidence(
            artistNorm = artistNorm,
            combinedText = combinedText,
            songs = listOf(song),
            durationMs = song.duration,
            cachedTag = null
        ).first
    }

    /**
     * Classifies an artist by fusing curated artist identity, per-song voting,
     * lexical/duration signals, and optional verified online metadata [cachedTag].
     */
    fun classifyArtist(
        artistName: String,
        songs: List<Song>,
        cachedTag: String? = null
    ): GenrePrediction {
        val artistNorm = foldAscii(artistName)
        val combinedText = buildString {
            append(artistNorm)
            songs.take(30).forEach { s ->
                append(' ')
                append(foldAscii(s.title))
                append(' ')
                append(foldAscii(s.album))
            }
        }
        val avgDuration = songs.map { it.duration }.filter { it > 0L }
            .let { if (it.isEmpty()) 0L else it.average().toLong() }
        return scoreEvidence(
            artistNorm = artistNorm,
            combinedText = combinedText,
            songs = songs,
            durationMs = avgDuration,
            cachedTag = cachedTag
        ).first
    }

    /**
     * Looks up cached online genre tag synchronously (no network).
     */
    fun getCachedPrediction(
        context: Context,
        artistName: String,
        songs: List<Song>
    ): GenrePrediction {
        val prefs = context.getSharedPreferences(PREFS_GENRE_CACHE, Context.MODE_PRIVATE)
        readManualOverride(prefs, artistName)?.let { return it }
        val cachedTag = readValidCacheTag(prefs, artistName)
        return classifyArtist(artistName, songs, cachedTag = cachedTag)
    }

    /** Persist a manual genre correction so the ML engine remembers and propagates it across folders. */
    fun setManualArtistGenre(context: Context, artistName: String, genre: MusicGenre) {
        val key = OVERRIDE_KEY_PREFIX + foldAscii(artistName)
        context.getSharedPreferences(PREFS_GENRE_CACHE, Context.MODE_PRIVATE)
            .edit()
            .putString(key, genre.name)
            .apply()
    }

    fun clearManualArtistGenre(context: Context, artistName: String) {
        val key = OVERRIDE_KEY_PREFIX + foldAscii(artistName)
        context.getSharedPreferences(PREFS_GENRE_CACHE, Context.MODE_PRIVATE)
            .edit()
            .remove(key)
            .apply()
    }

    /**
     * Computes a full genre breakdown, dominant genre, and mood/vibe summary for [folder],
     * using folder artists (even with 0 local tracks), albums, and resolved tracks.
     */
    fun analyzeFolderGenreProfile(
        folder: MusicFolder,
        allSongs: List<Song>,
        artistPredictions: Map<String, GenrePrediction> = emptyMap(),
        playCounts: Map<String, Int> = emptyMap(),
        downloadedIds: Set<String> = emptySet()
    ): FolderGenreProfile {
        val songsByArtist = allSongs.groupBy { foldAscii(it.artist) }
        val directSongs = CustomFolderTree.resolveFolderSongs(folder, allSongs, playCounts, downloadedIds)
        val countsByGenre = mutableMapOf<MusicGenre, Int>()
        val weightByGenre = mutableMapOf<MusicGenre, Double>()
        val folderThemePred = folderThemePrior(folder.name)

        // 1. Score explicit folder artists (highest structural signal inside a folder).
        folder.artistNames.forEach { artist ->
            val artistKey = foldAscii(artist)
            val basePred = artistPredictions[artist]
                ?: classifyArtist(artist, songsByArtist[artistKey].orEmpty())
            val effective = if (!basePred.isVerifiedTag && basePred.confidence < 0.45f && folderThemePred != null) {
                folderThemePred
            } else {
                basePred
            }
            countsByGenre[effective.genre] = (countsByGenre[effective.genre] ?: 0) + 1
            weightByGenre[effective.genre] = (weightByGenre[effective.genre] ?: 0.0) + 1.5 * effective.confidence.toDouble()
        }

        // 2. Score resolved songs not already dominated by explicit folder artists.
        val explicitArtistKeys = folder.artistNames.mapTo(mutableSetOf()) { foldAscii(it) }
        directSongs.forEach { song ->
            val songPred = classifySong(song)
            val effective = if (songPred.confidence < 0.45f) {
                artistPredictions[song.artist]
                    ?: folderThemePred
                    ?: songPred
            } else {
                songPred
            }
            val trackWeight = if (foldAscii(song.artist) in explicitArtistKeys) 0.4 else 1.0
            countsByGenre[effective.genre] = (countsByGenre[effective.genre] ?: 0) + 1
            weightByGenre[effective.genre] = (weightByGenre[effective.genre] ?: 0.0) + trackWeight * effective.confidence.toDouble()
        }

        // 3. Score standalone albums that have no local tracks resolved yet.
        folder.albumNames.forEach { album ->
            val matchingSongs = allSongs.filter { foldAscii(it.album) == foldAscii(album) }
            if (matchingSongs.isEmpty()) {
                val fakeSong = Song(
                    id = album,
                    title = album,
                    artist = folder.artistNames.firstOrNull().orEmpty(),
                    album = album,
                    duration = 0L
                )
                val albumPred = classifySong(fakeSong).let {
                    if (it.confidence < 0.45f && folderThemePred != null) folderThemePred else it
                }
                countsByGenre[albumPred.genre] = (countsByGenre[albumPred.genre] ?: 0) + 1
                weightByGenre[albumPred.genre] = (weightByGenre[albumPred.genre] ?: 0.0) + albumPred.confidence.toDouble()
            }
        }

        val totalWeight = weightByGenre.values.sum().coerceAtLeast(0.0)
        val totalItems = folder.artistNames.size + directSongs.size
        if (totalWeight <= 0.0) {
            return FolderGenreProfile(
                dominantGenre = folderThemePred?.genre,
                dominantConfidence = folderThemePred?.confidence ?: 0f,
                breakdown = emptyList(),
                totalItems = totalItems,
                vibeSummary = folderThemePred?.let { vibeLabelFor(it.genre) } ?: "Klasör analizi için sanatçı veya şarkı ekleyin"
            )
        }

        val breakdown = weightByGenre.entries
            .sortedByDescending { it.value }
            .map { (genre, w) ->
                FolderGenreBreakdownItem(
                    genre = genre,
                    count = countsByGenre[genre] ?: 1,
                    percentage = ((w / totalWeight) * 100.0).toInt().coerceIn(1, 100)
                )
            }
        val dominant = breakdown.firstOrNull()?.genre
        val dominantConf = dominant?.let { (weightByGenre[it]!! / totalWeight).toFloat().coerceIn(0.30f, 0.99f) } ?: 0f
        val vibe = if (dominant != null) vibeLabelFor(dominant) else "Karma Klasör"
        return FolderGenreProfile(
            dominantGenre = dominant,
            dominantConfidence = dominantConf,
            breakdown = breakdown,
            totalItems = totalItems,
            vibeSummary = vibe
        )
    }

    /**
     * Plans a smart automatic split of [folder] into genre-based subfolders when 2+ distinct genres exist.
     */
    fun planGenreSubfolderSplit(
        folder: MusicFolder,
        allSongs: List<Song>,
        artistPredictions: Map<String, GenrePrediction> = emptyMap()
    ): List<GenreSubfolderSplit> {
        val songsByArtist = allSongs.groupBy { foldAscii(it.artist) }
        val songsById = allSongs.associateBy { it.id }

        val artistsByGenre = linkedMapOf<MusicGenre, MutableList<String>>()
        val albumsByGenre = linkedMapOf<MusicGenre, MutableList<String>>()
        val songsByGenre = linkedMapOf<MusicGenre, MutableList<String>>()

        folder.artistNames.forEach { artist ->
            val pred = artistPredictions[artist] ?: classifyArtist(artist, songsByArtist[foldAscii(artist)].orEmpty())
            artistsByGenre.getOrPut(pred.genre) { mutableListOf() }.add(artist)
        }
        folder.albumNames.forEach { album ->
            val matching = allSongs.filter { foldAscii(it.album) == foldAscii(album) }
            val genre = matching.firstOrNull()?.let { artistPredictions[it.artist]?.genre ?: classifySong(it).genre }
                ?: classifyArtist(album, matching).genre
            albumsByGenre.getOrPut(genre) { mutableListOf() }.add(album)
        }
        folder.songIds.forEach { id ->
            val song = songsById[id] ?: return@forEach
            val songPred = classifySong(song)
            val genre = if (songPred.confidence >= 0.55f) songPred.genre
            else artistPredictions[song.artist]?.genre ?: songPred.genre
            songsByGenre.getOrPut(genre) { mutableListOf() }.add(id)
        }

        val allGenres = (artistsByGenre.keys + albumsByGenre.keys + songsByGenre.keys).distinct()
        if (allGenres.size < 2) return emptyList()

        return allGenres.map { genre ->
            GenreSubfolderSplit(
                genre = genre,
                folderName = genre.displayNameTr,
                artists = artistsByGenre[genre].orEmpty().distinct(),
                albums = albumsByGenre[genre].orEmpty().distinct(),
                songIds = songsByGenre[genre].orEmpty().distinct()
            )
        }.sortedByDescending { it.artists.size + it.albums.size + it.songIds.size }
    }

    private fun folderThemePrior(folderName: String): GenrePrediction? {
        val norm = foldAscii(folderName)
        if (norm.length < 3) return null
        val (pred, _) = scoreEvidence(
            artistNorm = "",
            combinedText = norm,
            songs = emptyList(),
            durationMs = 0L,
            cachedTag = null
        )
        return if (pred.confidence >= 0.45f) {
            pred.copy(
                confidence = 0.64f,
                matchedSignals = listOf("Klasör teması: ${folderName.trim()}")
            )
        } else null
    }

    private fun vibeLabelFor(genre: MusicGenre): String = when (genre) {
        MusicGenre.AnadoluRock -> "Yüksek Enerji • Türkçe Rock & Gitar"
        MusicGenre.RockMetal -> "Güçlü Ritim • Rock & Metal Riff'leri"
        MusicGenre.Pop -> "Akıcı & Modern • Pop Hit Odağı"
        MusicGenre.RapHipHop -> "Ritmik & Flow • Rap / Hip-Hop"
        MusicGenre.ElectronicDance -> "Kulüp & Tempo • Elektronik / Dance"
        MusicGenre.LofiChill -> "Odak & Gece • Lo-Fi / Sakin Akış"
        MusicGenre.AcousticIndie -> "Duygusal & Doğal • Akustik / Indie"
        MusicGenre.ArabeskFantezi -> "Damar & Taverna • Arabesk / Fantezi"
        MusicGenre.JazzBlues -> "Sıcak & Sofistike • Jazz / Blues / Soul"
        MusicGenre.ClassicalInstrumental -> "Enstrümantal & Derin • Klasik / Tema"
    }

    private fun readManualOverride(prefs: android.content.SharedPreferences, artistName: String): GenrePrediction? {
        val genreName = prefs.getString(OVERRIDE_KEY_PREFIX + foldAscii(artistName), null) ?: return null
        val genre = MusicGenre.entries.firstOrNull { it.name == genreName } ?: return null
        return GenrePrediction(
            genre = genre,
            confidence = 1.0f,
            matchedSignals = listOf("Kullanıcı doğrulaması"),
            isVerifiedTag = true
        )
    }

    /**
     * Enriches [artistNames] using TR-first / US-fallback iTunes Artist/Track metadata (cached 30 days),
     * bounded concurrency in batches of 4, plus folder co-occurrence k-NN propagation.
     */
    suspend fun enrichArtists(
        context: Context,
        artistNames: List<String>,
        allSongs: List<Song>,
        folders: List<MusicFolder> = emptyList(),
        onProgress: (Map<String, GenrePrediction>) -> Unit = {}
    ): Map<String, GenrePrediction> {
        val distinct = artistNames.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (distinct.isEmpty()) return emptyMap()

        val songsByArtist = allSongs.groupBy { foldAscii(it.artist) }
        val prefs = context.getSharedPreferences(PREFS_GENRE_CACHE, Context.MODE_PRIVATE)
        val result = LinkedHashMap<String, GenrePrediction>()
        val needNetwork = mutableListOf<String>()

        distinct.forEach { name ->
            val key = foldAscii(name)
            val manual = readManualOverride(prefs, name)
            if (manual != null) {
                result[name] = manual
                return@forEach
            }
            val cachedTag = readValidCacheTag(prefs, name)
            val hasCacheEntry = hasFreshCacheEntry(prefs, name)
            val artistSongs = songsByArtist[key].orEmpty()
            val (pred, hasStrongCuratedMatch) = scoreEvidence(
                artistNorm = key,
                combinedText = buildCombinedArtistText(key, artistSongs),
                songs = artistSongs,
                durationMs = artistSongs.map { it.duration }.filter { it > 0L }
                    .let { if (it.isEmpty()) 0L else it.average().toLong() },
                cachedTag = cachedTag
            )
            result[name] = pred
            // Skip network lookup if we already have a strong curated match or fresh cache hit/miss.
            if (!hasCacheEntry && !hasStrongCuratedMatch && !pred.isVerifiedTag) {
                needNetwork += name
            }
        }

        applyFolderKnnPropagation(result, folders, songsByArtist)
        onProgress(result.toMap())

        if (needNetwork.isNotEmpty()) {
            val targets = needNetwork.take(48)
            for (batch in targets.chunked(4)) {
                val batchResults: List<Pair<String, String?>> = withContext(Dispatchers.IO) {
                    coroutineScope {
                        batch.map { name ->
                            async { name to fetchOnlineGenreTag(name) }
                        }.awaitAll()
                    }
                }
                val editor = prefs.edit()
                val now = System.currentTimeMillis()
                batchResults.forEach { (name, fetchedTag) ->
                    val cacheKey = CACHE_KEY_PREFIX + foldAscii(name)
                    if (!fetchedTag.isNullOrBlank()) {
                        editor.putString(cacheKey, "$now|$fetchedTag")
                        val artistSongs = songsByArtist[foldAscii(name)].orEmpty()
                        result[name] = classifyArtist(name, artistSongs, fetchedTag)
                    } else {
                        editor.putString(cacheKey, "$now|$CACHE_MISS_MARKER")
                    }
                }
                editor.apply()
                applyFolderKnnPropagation(result, folders, songsByArtist)
                onProgress(result.toMap())
            }
        }
        return result
    }

    /**
     * Groups [artistNames] into smart genre clusters ordered by cluster size and confidence.
     */
    fun clusterArtistsByGenre(
        artistNames: List<String>,
        allSongs: List<Song>,
        precomputed: Map<String, GenrePrediction> = emptyMap(),
        folders: List<MusicFolder> = emptyList()
    ): List<ArtistGenreCluster> {
        if (artistNames.isEmpty()) return emptyList()
        val songsByArtist = allSongs.groupBy { foldAscii(it.artist) }
        val working = LinkedHashMap<String, GenrePrediction>()

        artistNames.distinct().forEach { name ->
            working[name] = precomputed[name] ?: classifyArtist(name, songsByArtist[foldAscii(name)].orEmpty())
        }
        applyFolderKnnPropagation(working, folders, songsByArtist)

        val grouped = LinkedHashMap<MusicGenre, MutableList<Pair<String, GenrePrediction>>>()
        working.forEach { (name, pred) ->
            grouped.getOrPut(pred.genre) { mutableListOf() }.add(name to pred)
        }

        return grouped.entries.map { (genre, entries) ->
            val totalSongs = entries.sumOf { (name, _) ->
                songsByArtist[foldAscii(name)].orEmpty().size
            }
            val avgConf = entries.map { it.second.confidence }.average().toFloat()
            ArtistGenreCluster(
                genre = genre,
                artists = entries.map { it.first }.sortedBy { it.lowercase(trLocale) },
                totalSongs = totalSongs,
                averageConfidence = avgConf
            )
        }.sortedWith(
            compareByDescending<ArtistGenreCluster> { it.artists.size }
                .thenByDescending { it.averageConfidence }
        )
    }

    private fun readValidCacheTag(prefs: android.content.SharedPreferences, artistName: String): String? {
        val raw = prefs.getString(CACHE_KEY_PREFIX + foldAscii(artistName), null) ?: return null
        val parts = raw.split("|", limit = 2)
        if (parts.size < 2) return null
        val ts = parts[0].toLongOrNull() ?: 0L
        val tag = parts[1]
        if (tag == CACHE_MISS_MARKER || tag.isBlank()) return null
        return if (System.currentTimeMillis() - ts <= CACHE_TTL_HIT_MS) tag else null
    }

    private fun hasFreshCacheEntry(prefs: android.content.SharedPreferences, artistName: String): Boolean {
        val raw = prefs.getString(CACHE_KEY_PREFIX + foldAscii(artistName), null) ?: return false
        val parts = raw.split("|", limit = 2)
        if (parts.size < 2) return false
        val ts = parts[0].toLongOrNull() ?: 0L
        val tag = parts[1]
        val age = System.currentTimeMillis() - ts
        return if (tag == CACHE_MISS_MARKER) {
            age <= CACHE_TTL_MISS_MS
        } else {
            tag.isNotBlank() && age <= CACHE_TTL_HIT_MS
        }
    }

    private fun buildCombinedArtistText(artistNorm: String, songs: List<Song>): String {
        return buildString {
            append(artistNorm)
            songs.take(30).forEach { s ->
                append(' ')
                append(foldAscii(s.title))
                append(' ')
                append(foldAscii(s.album))
            }
        }
    }

    private fun applyFolderKnnPropagation(
        predictions: MutableMap<String, GenrePrediction>,
        folders: List<MusicFolder>,
        songsByArtist: Map<String, List<Song>>
    ) {
        if (folders.isEmpty()) return
        folders.forEach { folder ->
            val themePrior = folderThemePrior(folder.name)
            val genreVotes = mutableMapOf<MusicGenre, Double>()
            if (themePrior != null) {
                genreVotes[themePrior.genre] = 0.75
            }
            if (folder.artistNames.size >= 2) {
                folder.artistNames.forEach { member ->
                    val p = predictions[member]
                        ?: classifyArtist(member, songsByArtist[foldAscii(member)].orEmpty())
                    if (p.confidence >= 0.56f) {
                        val voteWeight = if (p.isVerifiedTag && p.confidence >= 0.99f) 1.5 else p.confidence.toDouble()
                        genreVotes[p.genre] = (genreVotes[p.genre] ?: 0.0) + voteWeight
                    }
                }
            }
            val dominant = genreVotes.maxByOrNull { it.value } ?: return@forEach
            val isFolderThemeOnly = folder.artistNames.size < 2 && themePrior != null
            if (!isFolderThemeOnly && dominant.value < 0.8) return@forEach

            folder.artistNames.forEach { member ->
                val cur = predictions[member] ?: return@forEach
                if (!cur.isVerifiedTag && cur.confidence < 0.52f) {
                    val signal = if (isFolderThemeOnly) {
                        "Klasör teması (${folder.name})"
                    } else {
                        "Klasör ortaklığı (${folder.name})"
                    }
                    predictions[member] = GenrePrediction(
                        genre = dominant.key,
                        confidence = if (isFolderThemeOnly) 0.64f else 0.72f,
                        matchedSignals = listOf(signal),
                        isVerifiedTag = false
                    )
                }
            }
        }
    }

    private fun fetchOnlineGenreTag(artistName: String): String? {
        val targetNorm = foldAscii(artistName)
        if (targetNorm.length < 2) return null
        return try {
            // 1. iTunes Turkey store (best coverage for Turkish Pop/Rap/Rock/Arabesk),
            // 2. iTunes US catalog,
            // 3. Deezer public catalog fallback for niche/underground/electronic artists.
            fetchItunesForCountry(artistName, targetNorm, "TR")
                ?: fetchItunesForCountry(artistName, targetNorm, "US")
                ?: fetchDeezerGenreTag(artistName, targetNorm)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchDeezerGenreTag(artistName: String, targetNorm: String): String? {
        val encoded = URLEncoder.encode(artistName.trim(), "UTF-8")
        val targetTokens = targetNorm.split(tokenSplitRegex).filter { it.isNotEmpty() }.toSet()
        val url = "https://api.deezer.com/search/album?q=$encoded&limit=6"
        return try {
            httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string().orEmpty()
                val data = JSONObject(body).optJSONArray("data") ?: return null
                val counts = mutableMapOf<String, Int>()
                for (i in 0 until data.length()) {
                    val album = data.optJSONObject(i) ?: continue
                    val artistObj = album.optJSONObject("artist") ?: continue
                    val foundNorm = foldAscii(artistObj.optString("name"))
                    if (!isStrictArtistMatch(foundNorm, targetNorm, targetTokens)) continue
                    val genreId = album.optInt("genre_id", -1)
                    val tag = deezerGenreIdToTag(genreId) ?: continue
                    counts[tag] = (counts[tag] ?: 0) + 1
                }
                counts.maxByOrNull { it.value }?.key
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    private fun deezerGenreIdToTag(genreId: Int): String? = when (genreId) {
        132 -> "Pop"
        116 -> "Hip-Hop/Rap"
        152 -> "Rock"
        464 -> "Metal"
        106 -> "Electronic"
        113 -> "Dance"
        85 -> "Alternative"
        129 -> "Jazz"
        98 -> "Classical"
        466 -> "Folk"
        165, 169 -> "R&B/Soul"
        153 -> "Blues"
        else -> null
    }

    private fun fetchItunesForCountry(artistName: String, targetNorm: String, country: String): String? {
        val encoded = URLEncoder.encode(artistName.trim(), "UTF-8")
        val targetTokens = targetNorm.split(tokenSplitRegex).filter { it.isNotEmpty() }.toSet()

        val artistUrl = "https://itunes.apple.com/search?term=$encoded&country=$country&entity=musicArtist&limit=5"
        httpClient.newCall(Request.Builder().url(artistUrl).get().build()).execute().use { resp ->
            if (resp.isSuccessful) {
                val body = resp.body?.string().orEmpty()
                val results = JSONObject(body).optJSONArray("results")
                if (results != null) {
                    var bestContainsGenre: String? = null
                    for (i in 0 until results.length()) {
                        val obj = results.optJSONObject(i) ?: continue
                        val foundNorm = foldAscii(obj.optString("artistName"))
                        val genre = obj.optString("primaryGenreName").trim()
                        if (genre.isBlank()) continue
                        if (foundNorm == targetNorm) return genre
                        if (bestContainsGenre == null && isStrictArtistMatch(foundNorm, targetNorm, targetTokens)) {
                            bestContainsGenre = genre
                        }
                    }
                    if (bestContainsGenre != null) return bestContainsGenre
                }
            }
        }

        // Song entity fallback: strictly verify track artistName matches target artist before voting.
        val songUrl = "https://itunes.apple.com/search?term=$encoded&country=$country&entity=song&limit=8"
        httpClient.newCall(Request.Builder().url(songUrl).get().build()).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string().orEmpty()
            val results = JSONObject(body).optJSONArray("results") ?: return null
            val counts = mutableMapOf<String, Int>()
            for (i in 0 until results.length()) {
                val obj = results.optJSONObject(i) ?: continue
                val foundNorm = foldAscii(obj.optString("artistName"))
                if (!isStrictArtistMatch(foundNorm, targetNorm, targetTokens)) continue
                val g = obj.optString("primaryGenreName").trim()
                if (g.isNotBlank()) counts[g] = (counts[g] ?: 0) + 1
            }
            return counts.maxByOrNull { it.value }?.key
        }
    }

    private fun isStrictArtistMatch(
        foundNorm: String,
        targetNorm: String,
        targetTokens: Set<String>
    ): Boolean {
        if (foundNorm.isBlank() || targetNorm.isBlank()) return false
        if (foundNorm == targetNorm) return true
        val foundTokens = foundNorm.split(tokenSplitRegex).filter { it.isNotEmpty() }.toSet()
        return targetTokens.isNotEmpty() && foundTokens.containsAll(targetTokens) && foundTokens.size <= targetTokens.size + 2
    }

    private fun mapExternalGenrePrior(
        rawTag: String,
        artistNorm: String,
        combinedText: String
    ): List<Pair<MusicGenre, Double>> {
        val tag = foldAscii(rawTag)
        val hasTurkishContext = combinedText.any { it in "çğıöşü" } ||
            looksTurkishAscii(artistNorm) ||
            looksTurkishAscii(combinedText)
        return when {
            tag.contains("anatolian") || tag.contains("anadolu") || tag.contains("turkish rock") ->
                listOf(MusicGenre.AnadoluRock to 5.6, MusicGenre.RockMetal to 1.6)
            tag.contains("turkish rap") || tag.contains("turkce rap") ||
                tag.contains("hip-hop") || tag.contains("hip hop") || tag.contains("rap") ->
                listOf(MusicGenre.RapHipHop to 5.4)
            tag.contains("metal") || tag.contains("hard rock") || tag.contains("punk") || tag.contains("grunge") ->
                listOf(MusicGenre.RockMetal to 5.2)
            tag == "rock" ->
                if (hasTurkishContext) {
                    listOf(MusicGenre.RockMetal to 3.4, MusicGenre.AnadoluRock to 2.6)
                } else {
                    listOf(MusicGenre.RockMetal to 5.0)
                }
            tag.contains("alternative") || tag.contains("alternatif") || tag.contains("indie") ->
                if (hasTurkishContext) {
                    listOf(MusicGenre.AcousticIndie to 3.4, MusicGenre.AnadoluRock to 2.6, MusicGenre.RockMetal to 2.0)
                } else {
                    listOf(MusicGenre.AcousticIndie to 3.6, MusicGenre.RockMetal to 2.8)
                }
            tag.contains("electronic") || tag.contains("elektronik") || tag.contains("dance") ||
                tag.contains("house") || tag.contains("techno") || tag.contains("trance") ->
                listOf(MusicGenre.ElectronicDance to 5.0, MusicGenre.Pop to 1.4)
            tag.contains("lo-fi") || tag.contains("lofi") || tag.contains("ambient") || tag.contains("chill") ->
                listOf(MusicGenre.LofiChill to 5.0)
            tag.contains("singer/songwriter") || tag.contains("acoustic") || tag.contains("akustik") ->
                listOf(MusicGenre.AcousticIndie to 5.0)
            tag.contains("arabesque") || tag.contains("arabesk") || tag.contains("fantezi") ||
                tag.contains("turk sanat") || tag.contains("sanat muzigi") ->
                listOf(MusicGenre.ArabeskFantezi to 5.6)
            tag.contains("folk") || tag.contains("halk") ->
                if (hasTurkishContext) listOf(MusicGenre.ArabeskFantezi to 4.4, MusicGenre.AnadoluRock to 2.0)
                else listOf(MusicGenre.AcousticIndie to 4.6)
            tag.contains("world") ->
                if (hasTurkishContext) listOf(MusicGenre.ArabeskFantezi to 2.8, MusicGenre.Pop to 2.2)
                else listOf(MusicGenre.Pop to 2.4, MusicGenre.AcousticIndie to 1.8)
            tag.contains("jazz") || tag.contains("caz") || tag.contains("blues") ||
                tag.contains("soul") || tag.contains("r&b") ->
                listOf(MusicGenre.JazzBlues to 4.8, MusicGenre.Pop to 1.2)
            tag.contains("classical") || tag.contains("klasik") || tag.contains("instrumental") ||
                tag.contains("enstrumantal") || tag.contains("soundtrack") ->
                listOf(MusicGenre.ClassicalInstrumental to 5.2)
            tag.contains("pop") || tag.contains("latin") ->
                listOf(MusicGenre.Pop to 4.6)
            else -> emptyList()
        }
    }

    private fun looksTurkishAscii(norm: String): Boolean {
        val markers = listOf("ve ", "bir ", "ask", "kalp", "gece", "seni", "beni", "yok", "yar", "gibi", "degil", "olmaz", "son", "uzak")
        return markers.any { containsWholePhrase(norm, it.trim()) }
    }

    private fun scoreEvidence(
        artistNorm: String,
        combinedText: String,
        songs: List<Song>,
        durationMs: Long,
        cachedTag: String?
    ): Pair<GenrePrediction, Boolean> {
        val rawScores = DoubleArray(prototypes.size)
        val signalsPerGenre = Array(prototypes.size) { mutableListOf<String>() }
        var hasStrongArtistMatch = false

        val artistCandidates = buildList {
            if (artistNorm.isNotBlank()) add(artistNorm)
            artistNorm.split(collabSplitRegex)
                .map { it.trim() }
                .filter { it.length >= 2 }
                .forEach { add(it) }
        }.distinct()

        // 1. Curated artist identity scoring across primary name & collaborators.
        prototypes.forEachIndexed { idx, proto ->
            val matchedArtist = artistCandidates.any { candidate ->
                proto.strongArtists.any { known ->
                    candidate == known || containsWholePhrase(candidate, known)
                }
            }
            if (matchedArtist) {
                rawScores[idx] += 6.5
                hasStrongArtistMatch = true
                signalsPerGenre[idx].add("Sanatçı profili")
            }
        }

        // 2. Lexical phrase & token evidence weighted carefully across tracks.
        val phraseHitCounts = mutableMapOf<String, Int>()
        val tokenHitCounts = mutableMapOf<String, Int>()
        val scanUnits = if (songs.size > 1) {
            songs.take(30).map { foldAscii("${it.title} ${it.album}") }
        } else {
            listOf(combinedText)
        }

        scanUnits.forEach { unit ->
            val unitTokens = unit.split(tokenSplitRegex).filter { it.isNotEmpty() }.toSet()
            prototypes.forEach { proto ->
                proto.phraseWeights.keys.forEach { phrase ->
                    if (containsWholePhrase(unit, phrase)) {
                        phraseHitCounts[phrase] = (phraseHitCounts[phrase] ?: 0) + 1
                    }
                }
                proto.tokenWeights.keys.forEach { kw ->
                    if (kw in unitTokens) {
                        tokenHitCounts[kw] = (tokenHitCounts[kw] ?: 0) + 1
                    }
                }
            }
        }

        val trackScale = if (songs.size > 1) 1.0 + ln(songs.size.coerceAtMost(30).toDouble()) else 1.0
        prototypes.forEachIndexed { idx, proto ->
            proto.phraseWeights.forEach { (phrase, weight) ->
                val count = phraseHitCounts[phrase] ?: 0
                if (count > 0) {
                    val contrib = weight * (1.0 + 0.55 * ln(count.toDouble())) / ln(trackScale + 1.71828)
                    rawScores[idx] += contrib
                    signalsPerGenre[idx].add(phrase)
                }
            }
            proto.tokenWeights.forEach { (kw, weight) ->
                val count = tokenHitCounts[kw] ?: 0
                if (count > 0) {
                    val contrib = weight * (1.0 + 0.45 * ln(count.toDouble())) / ln(trackScale + 1.71828)
                    rawScores[idx] += contrib
                    signalsPerGenre[idx].add(kw)
                }
            }
            val range = proto.preferredDurationRangeMs
            if (range != null && durationMs > 0L && durationMs in range) {
                rawScores[idx] += 0.35
            }
        }

        // 3. Verified external genre metadata fusion (soft logit boost so AnadoluRock stays AnadoluRock).
        var isVerified = false
        if (!cachedTag.isNullOrBlank()) {
            val priors = mapExternalGenrePrior(cachedTag, artistNorm, combinedText)
            if (priors.isNotEmpty()) {
                isVerified = true
                priors.forEach { (genre, weight) ->
                    val idx = protoIndexByGenre[genre] ?: return@forEach
                    rawScores[idx] += weight
                    if ("Doğrulanmış tür: $cachedTag" !in signalsPerGenre[idx]) {
                        signalsPerGenre[idx].add(0, "Doğrulanmış tür: $cachedTag")
                    }
                }
            }
        }

        val maxScore = rawScores.maxOrNull() ?: 0.0
        if (maxScore <= 0.4) {
            return GenrePrediction(
                genre = MusicGenre.Pop,
                confidence = 0.28f,
                matchedSignals = listOf("Genel prior (veri sınırlı)"),
                isVerifiedTag = false
            ) to false
        }

        val expScores = DoubleArray(prototypes.size) { i ->
            exp((rawScores[i] - maxScore).coerceAtLeast(-20.0))
        }
        val sumExp = expScores.sum().coerceAtLeast(1e-9)
        var bestIdx = 0
        var bestProb = 0.0
        expScores.forEachIndexed { i, e ->
            val p = e / sumExp
            if (p > bestProb) {
                bestProb = p
                bestIdx = i
            }
        }
        return GenrePrediction(
            genre = prototypes[bestIdx].genre,
            confidence = bestProb.toFloat().coerceIn(0.22f, 0.99f),
            matchedSignals = signalsPerGenre[bestIdx].distinct().take(4),
            isVerifiedTag = isVerified
        ) to hasStrongArtistMatch
    }

    private fun containsWholePhrase(haystack: String, phrase: String): Boolean {
        val pattern = Regex("(^|[^a-z0-9])${Regex.escape(phrase)}([^a-z0-9]|$)")
        return pattern.containsMatchIn(haystack)
    }

    private fun foldAscii(text: String): String {
        val lower = text.trim().lowercase(trLocale)
        val sb = StringBuilder(lower.length)
        for (ch in lower) {
            when (ch) {
                '\u0307' -> Unit
                'ç' -> sb.append('c')
                'ğ' -> sb.append('g')
                'ı', 'İ', 'í', 'î', 'ì', 'ï' -> sb.append('i')
                'ö', 'ó', 'ò', 'ô', 'õ', 'ø' -> sb.append('o')
                'ş' -> sb.append('s')
                'ü', 'û', 'ú', 'ù' -> sb.append('u')
                'â', 'á', 'à', 'ä', 'ã', 'å' -> sb.append('a')
                'é', 'è', 'ê', 'ë' -> sb.append('e')
                'ñ' -> sb.append('n')
                else -> if (ch.category != CharCategory.NON_SPACING_MARK) sb.append(ch)
            }
        }
        return sb.toString()
    }
}
