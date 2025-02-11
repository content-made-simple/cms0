# Content Made Simple

# Goal

Make a system for users to post and share content: video, images, text (aka blogs or micro-blogs)

# Rationale

We want to create a solution that is simple for 
- users: no algorithms or AI.
- developers: written from scratch in Clojure.

# Approach

This is a slow build out project: we each spend an hour or two per week. 

We code the solution on a YouTube live stream and do some admin out of band. 

Thinking and researching the problems is currently out of band but that may evolve.

# Principles - lies we tell ourselves

Since this is, for the moment at least, primarily a development experiment, we will list development ~~lies~~ principles first.

This can, in the spirit of the project, be re-prioritised later.

## Development

### General

Aiming for the MISSED acronym, though we're not quite there yet. Or maybe we are.

- **Minimal**: reject code / framework that does more than we **explicitly** need at any moment.
  - We appreciate that what defines **needs** is slippery: 
    - do we need to test anything? do we need to reduce repetition?
    - we answer these in reference to the other principles and if they are not justified through those, we reject.
- **Safe**: ensure that the data is stored safely and cannot be lost.
- **Secure**: ensure that actions on data are secure.
- **Defer**: only take decisions at the [Last Responsible Moment](https://blog.codinghorror.com/the-last-responsible-moment/).

### Process

- Pair program
- Commit directly to main
- Review every time a namespace passes 200 lines, using that as our [Norris number](https://www.teamten.com/lawrence/writings/norris-numbers.html)
  - Clojure, being a LISP, is much denser and expressive than C-style languages so 10:1 (where 10 lines of another language = 1 line of Clojure) seems about right.
- Think about stuff and propose it to each other, even in code on the main branch.
  - Only use code that we have consensus around.

### Architecture

- Server-side rendering (defer)
  - HTML only, delivers an extremely basic UI. Evolve as needs must.
- Duratom as a persistent store (defer)
  - Content metadata
  - User data
- Object store
  - Content itself
- NGINX as a reverse proxy as root (secure)
  - Serve on port 80/443
- LetsEncrypt as a certificate source (secure)
  - Regular scheduled checks for renewal
- Clojure REPL without root (secure)
  - Accessible as a remote REPL via SSH tunnel

### Hosting - Exoscale
Ray works at [Exoscale](https://community.exoscale.com/platform/products-and-services/) and has a free allowance per month.

We use simple services that can be reproduced in other hosting environments, if and when it is needed.

Current set up:
- 1 x Medium size instance
  - 4 GB RAM, 2 CPUs
  - SSH access

These facilities are available and established though we have not yet used them.
- Attachable volume as a disk (safe)
  - for the `duratom` back end
- Bucket as an object store (safe)
  - for content

**NOTE:** This is case where **Defer** has won over **Safe**. As it stands, we only have our own videos as content so we can risk losing everything without any serious consequences. The videos are also posted on YouTube.

### Design

- Data oriented.

### Coding

- Functional programming.
  - Functional core, imperative shell.

### Quality control

- Pair programming
- REPL testing in production

### Deployment

- REPL evaluation in production

## Content

- We will serve what we are given
  - We will not optimize content. Any optimisations are the user's responsibility.
  - The system does not yet support for a variety of formats / sources. 
    - This could be added so that posters can have content that is relevant for a variety of devices.

## User data

TBD


# Authors

[Erik Assum](https://github.com/orgs/content-made-simple/people/slipset)

[Ray McDermott](https://github.com/orgs/content-made-simple/people/raymcdermott)

