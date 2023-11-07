# Contributing to MicroProfile Starter Extension for VS Code

We welcome contributions, and request you follow these guidelines.

 - [Raising issues](#raising-issues)
 - [Legal](#legal)
 - [Coding Standards](#coding-standards)
 - [Project Setup](#project-setup)

### Raising issues

Please raise any bug reports on the [issue tracker](https://github.com/MicroShed/microshed-testing/issues). Be sure to search the list to see if your issue has already been raised.

A good bug report is one that make it easy for us to understand what you were trying to do and what went wrong. Provide as much context as possible so we can try to recreate the issue.

### Legal

In order to make contribution as easy as possible, we follow the same approach as the [Developer's Certificate of Origin 1.1 (DCO)](https://developercertificate.org/) - that the Linux® Kernel [community](https://elinux.org/Developer_Certificate_Of_Origin) uses to manage code contributions.

We simply ask that when submitting a pull request for review, the developer
must include a sign-off statement in the commit message.

Here is an example Signed-off-by line, which indicates that the
submitter accepts the DCO:

```text
Signed-off-by: John Doe <john.doe@example.com>
```

You can include this automatically when you commit a change to your
local git repository using the following command:

```bash
git commit -s
```

### Coding Standards

This project contains a CI build that should run successfully.
Upon opening a Pull Request or Merging a commit this build will run.
If your Pull Request results in the failure of this build it will not be reviewed or merged until the failure is fixed.

### Project Setup

##### Requirements

- JDK 8 or higher
- Docker (daemon or desktop)
<!-- TODO support podman/colima for builds -->

##### Building project

Use the gradle wrapper to build this project:

```sh
./gradlew build
```
