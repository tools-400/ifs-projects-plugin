# IFS Projects

*Work on Code for IBM i projects (`iproj.json`) in RDi.*

IFS Projects is a plug-in for IBM Rational Developer for i (RDi). With it, you can work in RDi on
the same projects as with Code for IBM i in VS Code: projects whose source files are stream files
in the IFS (the file system of the IBM i) and that are described by a file `iproj.json`.

The plug-in adds to RDi:

* **IFS projects**: directories in the IFS, which you edit, build and check like a project.
* **Local IFS projects**: projects on your PC, e.g. a Git working copy. They are copied to the IFS
  and built there.
* **SSH terminal**: a command line (shell) of the IBM i in a view of RDi.
* **Export to a library**: the source files of a project are copied into source members, e.g. for
  a change management system.
* **Migration to an IFS project**: the members of source files are copied into a new IFS project,
  e.g. to move a utility from source members to Git and TOBi.

Like Code for IBM i, the plug-in builds with the build tools TOBi (`makei`) or GNU Make (`gmake`).
The compile errors are shown in the *Error List* view of RDi.

Further information about IFS Projects is available on the [IFS Projects Web Site](https://tools-400.github.io/ifs-projects/).

## Requirements

* RDi 9.6, 9.8 or 9.9 with a connection to the IBM i.
* The SSH server of the IBM i is running (`STRTCPSVR SERVER(*SSHD)`). It is needed for the
  terminal and for builds in the terminal.
* TOBi or GNU Make, installed from the open source packages of the IBM i.
* Recommended: bash as login shell, see [Command history](USAGE.md#command-history).

## Installation

In RDi, choose *Help > Install New Software...*, add the update site
https://tools-400.github.io/ifs-projects/update-site/eclipse/rdi8.0/ and install *IFS Projects*.
Restart RDi afterwards. Home page: https://tools-400.github.io/ifs-projects/.

The update site also offers the parsers of the headers, that build tools write into source members:
*IFS Projects STRPREPRC Headers* for `STRPREPRC`, *IFS Projects BUILD Headers* for the `BUILD`
command of Scott Klement, *IFS Projects COMPILE Headers* for the `COMPILE` command of Alan Campin
and *IFS Projects CPLCAS Headers* for the `CPLCAS` command of Denis Robitaille, see [Migration to an IFS project](USAGE.md#migration-to-an-ifs-project). Install the one of your
build tool, if your members have such headers. A build tool with another header can be added by a
plug-in of its own, see [EXTENDING.md](EXTENDING.md).

## Usage

[USAGE.md](USAGE.md) describes how to work with the plug-in: a quick start, the SSH terminal, IFS
projects and local IFS projects, the export to a library, the migration to an IFS project and
the preferences.

## For developers

How the plug-in works internally and how it is built: [DEVELOPMENT.md](DEVELOPMENT.md).
How to add a build tool with a plug-in of your own: [EXTENDING.md](EXTENDING.md).

## Liability

This software is free and provided without warranty. You use it at your own risk: test it with
non-critical data first, and back up your data before you use it. The [license](LICENSE) applies in
addition.
