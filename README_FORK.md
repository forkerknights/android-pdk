# Unofficial Pinterest SDK Fork

This document contains the updated information regarding the changes in
usage and functionality of this fork compared to the original.

## Changes

Currently, these are the changes implemented:

- Added support for [JitPack](https://jitpack.io/#forkerknights/android-pdk)
builds
- Externalized configuration for the API key. Now you just need to create
a file named `local_api.properties` on the root project with a property named
`clientId` <br>
Example: `clientId=12345678980`
- Implemented a silent login and provided a sample application using it.

## Planned changes

- Merge all exisiting PRs on the original repo to this one (After
reviewing them).
- Update the data model to add a class for the Pin's images

## How to contibute

Pull Requests are always welcome to this repo, as long as they comply
with the following guidelines:

- Do not open PRs against the `master` branch unless they are minor
changes. Preferably open them to the `dev_master` branch instead.
- Be sure to properly test your changes as much as possible before
opening a PR

Regarding issues with SDK, preferably open them on the
[official repo](https://github.com/pinterest/android-pdk). If you open
a issue on this fork, please keep it related to the changes implemented here.
Feature requests and questions related to the usage are also welcome.

## How to become a Forker Knight

Would you like to help me to maintain this project? You can become a
Forker Knight! As a member of the organization you will be able to review
and merge PRs on any repo owned by the organization, as well as pushing
commits directly to the repos.

In order to join the organization **one** of the following requirements
must be met:

- Submit 3 relevant PRs to any repo owned by the organization
- Contact me via a email and tell me your story (Your name, your
motivations, ...). In response I will also share my story, because that's how
trust is built.

Additionally, if you know about some good project that has been
discontinued and there isn't any good alternative, you can send me an
email to let me know. If I find the project interesting I might consider
your entrance to the organization.
