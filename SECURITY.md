# Security Policy

## Reporting a vulnerability

If you find a security issue in Sentüp, please report it privately rather than
in a public issue, so users can be protected before details spread.

Open a **private security advisory** on GitHub:
<https://github.com/caca1403/sentup/security/advisories/new>

…or email **cayircagatay337@gmail.com**. General bug reports and feature
suggestions can also be sent to that address.

Please include:
- A description and the affected version(s).
- Steps to reproduce.
- Any proof-of-concept, if you have one.

I aim to acknowledge reports within a few days and to ship a fix (or a reasoned
explanation) after that.

## Scope

This covers the Sentüp application source in this repository. Third-party
libraries are credited in `THIRD_PARTY_NOTICES.md`; report library issues
upstream.

## Note on credentials

Sentüp stores YouTube session cookies **only on your own device** and never
uploads them anywhere. If you believe a session token can leak to a third party,
that is exactly the kind of bug I want to hear about.
