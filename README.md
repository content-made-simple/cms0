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

# Principles

Since this is, for the moment at least, primarily a development experiment, we will list those principles first.

This can, in the spirit of the project, be re-prioritised later.

## Development

### General

Aiming for the MISSED or MESSED bacronym ... we're not quite there yet.

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

### Architecture

- Server-side rendering
  - HTML only, delivers an extremely basic UI on the Defer principle.
  - This is a pragmatic decision rather than a principle since this is likely to evolve.

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

