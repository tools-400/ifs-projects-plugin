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
* Recommended: bash as login shell, see [Command history](#command-history).

## Installation

In RDi, choose *Help > Install New Software...*, add the update site
https://tools-400.github.io/ifs-projects/update-site/eclipse/rdi8.0/ and install *IFS Projects*.
Restart RDi afterwards. Home page: https://tools-400.github.io/ifs-projects/.

The update site also offers the parsers of the headers, that build tools write into source members:
*IFS Projects STRPREPRC Headers* for `STRPREPRC`, *IFS Projects BUILD Headers* for the `BUILD`
command of Scott Klement, *IFS Projects COMPILE Headers* for the `COMPILE` command of Alan Campin
and *IFS Projects CPLCAS Headers* for the `CPLCAS` command of Denis Robitaille, see [Migration to an IFS project](#migration-to-an-ifs-project). Install the one of your
build tool, if your members have such headers. A build tool with another header can be added by a
plug-in of its own, see [EXTENDING.md](EXTENDING.md).

## Quick start

1. In the *Remote Systems* view, expand your IBM i connection, right-click *IFS Projects* and
   choose *New > IFS Project...*. Select the directory of the project in the IFS.
2. Right-click the project and choose *Build*. The build runs in an SSH terminal, which shows the
   output.
3. When the build has ended, the compile errors are shown in the *Error List* view. Double-click
   an error to open the source at that line.

You have cloned a project with Git to your PC? Import it with *File > Import... > IFS Projects >
Existing Local IFS Project*, see [Local IFS projects](#local-ifs-projects).

## SSH terminal

Open a terminal from the context menu of the *Remote Systems* view:

| Right-click on | Menu item | The terminal starts in |
|---|---|---|
| IBM i connection | *Open SSH Terminal* | your home directory |
| IFS directory | *Open SSH Terminal Here* | the directory |
| IFS file | *Open SSH Terminal Here* | the directory of the file |
| IFS filter with one filter string | *Open SSH Terminal Here* | the directory of the filter |
| IFS project | *Open SSH Terminal in Project Directory* | the project directory |

* All terminals are shown in the *SSH Terminal* view: the active terminal on the left, the list of
  all terminals on the right. A double-click on a disconnected terminal in the list connects it
  again. A terminal of the same connection and directory is reused;
  *Open New SSH Terminal ...* or the *New Terminal* button opens another one.
* If you have closed the view, open it with *Window > Show View > Other... > IFS Projects >
  SSH Terminal*.
* The terminals are closed, when RDi is closed. To get them back after a restart (not connected),
  select *Restore terminals after a restart* in the [preferences](#preferences).
* Log-on: with an SSH key, which is set up in *Window > Preferences > General > Network
  Connections > SSH2* (RSA or ECDSA keys in PEM format, no Ed25519 keys). Without a key, the
  terminal asks for the password, unless *Use the password saved for the RSE connection* is
  selected in the preferences.

### Keys

| Key | Function |
|---|---|
| Ctrl+C | Copies the selected text. Without a selection, it stops the running command. |
| Ctrl+V | Pastes the text of the clipboard. |
| Up, Down | Previous or next command (see below). |
| Shift+Page Up, Shift+Page Down | Scrolls back or forward by one page. |
| Ctrl+Shift+Home, Ctrl+Shift+End | Scrolls to the beginning or the end. |

While the terminal has the focus, all other keys go to the shell, e.g. Ctrl+A and Ctrl+E (start
and end of the line) or Ctrl+R (search the command history) of bash. On macOS, Cmd+C and Cmd+V copy
and paste; Ctrl+C stops the command.

### Command history

Up, Down and Ctrl+R need bash as login shell. bsh, the default shell of the IBM i, has no command
history. To make bash your login shell, install bash (open source package) and run this SQL
statement once, e.g. in *Run SQL Scripts*:

```sql
CALL QSYS2.SET_PASE_SHELL_INFO('*CURRENT', '/QOpenSys/pkgs/bin/bash');
```

With `'*DEFAULT'` instead of `'*CURRENT'`, bash becomes the login shell of all users without a
shell of their own. This requires the special authority *SECADM.

## IFS projects

An IFS project is a directory in the IFS with your source files. In the *Remote Systems* view,
each IBM i connection has the entry *IFS Projects*, which lists your projects. You open, edit and
compare their files as usual.

| Action (context menu) | Function |
|---|---|
| *New > IFS Project...* (on *IFS Projects* or on an IFS directory) | Creates the project. Optionally creates the directory and a new `iproj.json`. An existing `iproj.json` is not changed. The directory must not contain `$` or `#`, because make interprets them. |
| *Build* (project) | Builds the project with `buildCommand` of `iproj.json` or with the default build command of the preferences. |
| *Build* (file) | Compiles the file with `compileCommand` of `iproj.json` or with the default compile command. |
| *Check Syntax* (RPGLE, SQLRPGLE file) | Checks the source for errors without creating an object, see [Syntax check](#syntax-check). |
| *Open SSH Terminal in Project Directory* | Opens a terminal in the project directory. |
| *Export to Library...* (project) | Copies the source files into members of a library, see [Export to a library](#export-to-a-library). |
| *Properties > IFS Project* | Edits `iproj.json`. Settings that the page does not know are kept. |
| *Properties > IFS Project > Environment* | Environment variables of the project, see below. |

**Placeholders** in the compile command: `{filename}` and `{files}` (name of the file), `{host}`
(host name of the IBM i) and `{usrprf}` (user profile).

**Environment variables** hold settings that differ from system to system, e.g. library names.
They are stored in RDi, not in the project directory, and are set before each build. TOBi replaces
`&NAME` in `iproj.json` with the value of the variable `NAME`, e.g. `"setIBMiEnvCmd": ["&ENVCMD"]`
with `ENVCMD=CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)`.

### Where the build runs

You choose it with *Run build and compile commands* in the [preferences](#preferences):

* **In the SSH terminal of the project** (default): the terminal shows the output, and Ctrl+C stops
  the build. While a build is running, the next build opens a new terminal. The *Progress* view
  shows, that the plug-in waits for the end of the build; canceling it there only stops waiting,
  not the build.
* **In a job of the RSE connection**: the build runs in the background in a job of the RDi
  connection (RSE), without SSH. The output is shown in the *Commands Log* view, and *Cancel* in
  the *Progress* view stops the build. Builds of the same project run one after the other.

In both cases, the compile errors are shown in the *Error List* view afterwards. Exception: a
build command with a comment (`#`) or with `&` or `|` at the end runs unchanged in the terminal,
without *Error List*.

### Syntax check

*Check Syntax* compiles an RPGLE or SQLRPGLE source without creating an object and shows the errors
in the *Error List* view. It uses the library list and the include directories of `iproj.json`.
Unsaved changes are saved first, if you agree.

To check the source in the LPEX editor with a key, set up a user action once:

1. *Window > Preferences > LPEX Editor > User Actions*: add the action `ifsCheckSyntax` with the
   class `de.tools400.ifsprojects.lpex.CheckSyntaxAction`.
2. *LPEX Editor > User Key Actions*: assign a free key to `ifsCheckSyntax`.

Instead of a key, you can enter `action ifsCheckSyntax` on the LPEX command line.

## Local IFS projects

A local IFS project is a project in the RDi workspace, e.g. a Git working copy with `iproj.json`.
Before each build, the plug-in copies the changed files to a directory in the IFS (the deploy
directory) and builds the project there. Files are only copied from your PC to the IFS, never back.

| Action | Function |
|---|---|
| *File > Import... > IFS Projects > Existing Local IFS Project* | Imports a directory, e.g. a Git working copy: select the directory, then the IBM i connection and the deploy directory. An existing `iproj.json` is used unchanged. |
| *File > New > Project... > IFS Projects > Local IFS Project* | Creates a new local IFS project, optionally with a new `iproj.json`. |
| *Configure as IFS Project...* (context menu of a project with `iproj.json`) | Makes a project of the workspace a local IFS project. |
| *Properties > IFS Project* | Connection, deploy directory (default `<home>/.ifsprojects/builds/<project>`) and the settings of `iproj.json`. |
| *Build on IBM i* (project) | Saves the editors, copies the changes and builds the project like an IFS project. |
| *Build on IBM i* (file) | Copies all changes, so that changed copy books are used too, and compiles the file. |
| *Check Syntax* (RPGLE, SQLRPGLE file) | Copies the changes and checks the file. |
| *Upload Changes to IFS* | Copies new and changed files and deletes the files, that you have deleted. |
| *Upload All to IFS* | Copies all files, also unchanged ones, and overwrites files changed in the IFS without asking. |
| *Export to Library...* | Copies the files of the workspace into members of a library, see [Export to a library](#export-to-a-library). The deploy directory is not used. |
| *Properties > IFS Project > Environment* | Environment variables of the project. |

* *Text files in UTF-8* is recommended. RDi then stores the encoding in `.settings/` of the project.
* RDi creates the file `.project` in the project directory. If Git shall not show it and
  `.settings/` as new files, add both to `.git/info/exclude`.
* Not copied are the files listed in `.deployignore` or, if it does not exist, in `.gitignore` of
  the project directory (Git syntax), and `.git`, `.project`, `.settings` and the output folders of
  TOBi.
* If a file in the deploy directory has been changed by someone else, e.g. with Code for IBM i, you
  are asked, whether it may be overwritten. Other files of the deploy directory, e.g. the output of
  TOBi, are never deleted.
* The files are copied unchanged. A new stream file gets the CCSID of the encoding of the local
  file, e.g. 1208 for UTF-8.
* A double-click on an error in the *Error List* opens the local file.
* Limitation of RDi: the *Outline* view of LPEX does not find copy books in the `includePath`
  directories of `iproj.json`. Builds and syntax checks are not affected.

## Export to a library

*Export to Library...* in the context menu of an IFS project or a local IFS project copies the
source files of the project into source members of a library, e.g. for a change management system
that works with members. The files of the project are not changed. Local IFS projects are exported
from the workspace; unsaved changes are saved first, if you agree.

**Members.** The stream file `name.type` becomes the member `NAME` of the source type `TYPE`, the
reverse of `makei cvtsrcpf` of TOBi, e.g. `qrpglesrc/hello.pgm.rpgle` becomes `HELLO` of type
`RPGLE`. Copy books have the extension of their source type followed by `inc` and get that type,
e.g. `*.rpgleinc` (`RPGLE`), `*.sqlrpgleinc` (`SQLRPGLE`) and `*.clleinc` (`CLLE`); `*.clinc` of
Code for IBM i becomes `CLLE`, too. Only the file types that TOBi knows are exported; other files, e.g.
`iproj.json` or `Rules.mk`, are skipped. The same files as by the upload of local IFS projects are
excluded, e.g. the files of `.deployignore` or `.gitignore`.

**Source files**, to be chosen on the first page:

* *Name of the directory, otherwise by source type* (default): `qrpglesrc/hello.pgm.rpgle` goes to
  `QRPGLESRC`. Files in the project directory and in directories, whose names are not valid file
  names, are exported by source type.
* *By source type*: the default source file of the IBM command, that compiles the source, e.g.
  `QRPGLESRC` for RPGLE, `QCLSRC` for CLLE and `QDDSSRC` for DSPF.
* *One source file*: all files into one source file.

**New source files** get the CCSID of the first page and at least its record length (the defaults
are set in the [preferences](#preferences): 1141 and 112). A source file with longer lines gets a
longer record length. The CCSID must be an EBCDIC CCSID with one byte per character: select it from
the list or enter its number. If the library does not exist, you are asked, whether the export
shall create it.

**Include directives** are converted, so that the members compile from the source files:
`/copy 'inc/consts.rpgleinc'` becomes `/copy inc,consts` and `INCLUDE SRCSTMF('vars.clinc')`
becomes `INCLUDE SRCMBR(VARS) SRCFILE(QCLSRC)`. The copy book is searched like the compiler does:

* RPG (`/COPY`, `/INCLUDE`): in the project directory, the `includePath` directories of
  `iproj.json` and the directory of the source, also with the extensions `.rpgleinc` and `.rpgle`.
  A name without `/`, `.` and `,`, that is no file of the project, stays a member name.
* CL (`INCLUDE SRCSTMF`): in the directory of the source and the `includePath` directories.
* `EXEC SQL INCLUDE 'inc/hostvars.sqlrpgleinc'` becomes `EXEC SQL INCLUDE HOSTVARS`, also if the
  statement spans several lines or stands in fixed format between `C/EXEC SQL` and `C/END-EXEC`.
  The statement names only a member, its source file is the parameter `INCFILE` of the compile
  command, by default the source file of the compiled member. If the copy book goes into another
  source file, `INCFILE` has to be specified; the preview warns about it.
* Not converted are CL `INCLUDE` commands on several lines and copy books outside of the project.
  The preview shows them.

**Member text** from the comment `%TEXT` in the first lines of the file, as written by
`makei cvtsrcpf --text`:

```
// %METADATA
// %TEXT Display a message
// %EMETADATA
```

**Preview.** The second page lists the files with member, source type and status (*New*,
*Replace*, *Error*). Files with errors are not exported: a file name with more than 10 characters
before the first period, several files for the same member, a line longer than the record length of
an existing source file or a file of the same name that is not a source file. Warnings, e.g.
characters that do not exist in the CCSID of the source file or include directives that are not
converted, do not prevent the export. Select a file to see all its problems, the converted include
directives and the member text. You can deselect files.

A **double-click on a file** shows the source, that would be written into the member — the member
does not exist yet, so it cannot be opened in an editor:

* *Result*: the converted source in the viewer of the text editors, with line numbers and the text
  font of the workbench. *Find* is the search of the editors, on the button and on Ctrl+F.
  Replacing is not possible: the source is only shown, it is written by *Finish*.
* *Comparison*: the stream file of the project and the member side by side, so that the converted
  lines are easy to find. The search works here, too, and searches the side that has the focus.

**Export.** *Finish* starts the export in the background. Before an existing member is replaced,
you are asked: *Yes*, *Yes To All*, *No*, *No To All* or *Cancel*. The commands are shown in the
*Commands Log* view, a summary at the end. The next export of the project starts with the same
settings.

## Migration to an IFS project

*Migrate to IFS Project...* in the context menu of a library or of source files in the *Remote
Systems* view copies the members into a new IFS project, the reverse of the export. The members are
not changed.

**Source files.** On a library, the first page lists all its source files; on source files, only
the selected ones. All are selected; *Select All* and *Deselect All* are to the right of the list.
Options:

* *Member text as comment %TEXT*: inserts the member text at the beginning of the file, like
  `makei cvtsrcpf --text`. TOBi uses it as object text, the export reads it back.
* *Keep the RPG directives of the members for the member build*: see include directives below.
* *CCSID of source files with CCSID 65535*: the members of such files are read with this CCSID,
  because `CPYTOSTMF` does not convert them otherwise.

**Stream files.** The member `NAME` of type `TYPE` in the source file `QXXXSRC` becomes
`qxxxsrc/name.type`, like `makei cvtsrcpf --tolower`, e.g. `QRPGLESRC(HELLO)` becomes
`qrpglesrc/hello.rpgle`. A copy book, i.e. a member included by other members, gets the extension
`typeinc`, which TOBi recommends, e.g. `qcpysrc/consts.rpgleinc`. The files are written in UTF-8
(CCSID 1208).

**Include directives** are converted to the stream files:

* RPG: `/copy QCPYSRC,CONSTS`, `/copy MYLIB/QCPYSRC,CONSTS` and `/copy CONSTS` (source file
  `QRPGLESRC`) become `/copy 'qcpysrc/consts.rpgleinc'`, relative to the project directory, in
  which TOBi compiles. With the option to keep the member directives, the member build still works:

  ```
  /if defined(MBR_BUILD)
  /copy QCPYSRC,CONSTS
  /else
  /copy 'qcpysrc/consts.rpgleinc'
  /endif
  ```

  The build from the IFS needs no condition; the member build sets `DEFINE(MBR_BUILD)`, e.g. in the
  `STRPREPRC` header. The option applies to the SQL statements below, too.
* RPG with SQL: `exec sql include HOSTVARS` becomes
  `exec sql include 'qcpysrc/hostvars.sqlrpgleinc'`, also if the statement spans several lines or
  stands in fixed format between `C/EXEC SQL` and `C/END-EXEC`. The statement names only a member;
  its source file is the parameter `INCFILE` of the compiler, by default the source file of the
  compiled member. Because `INCFILE` names one source file, all statements of a member refer to the
  same one, which the migration determines:

  * `INCFILE` of the header of the member, if it has one and the feature of its build tool is
    installed, e.g. *IFS Projects STRPREPRC Headers*, and not switched off, see
    [Preferences](#preferences).
  * Otherwise the source file, that contains the members of all statements of the member: its own
    source file, which is the default of the compiler, otherwise the only other migrated source
    file, that contains them all. That all members have to be in one source file usually makes it
    unambiguous, e.g. if `HOSTVARS` exists twice, but only `QCPYSRC` contains `HOSTVARS` and
    `CURSORS` together.
  * If no or several source files contain them, the statements of the member are not converted and
    the preview shows it.
* CL: `INCLUDE SRCMBR(VARS) SRCFILE(QCPYSRC)` becomes `INCLUDE SRCSTMF('../qcpysrc/vars.clleinc')`,
  relative to the directory of the source, where CL searches it. Without `SRCFILE`, the member is in
  the source file of the including member.
* Not converted are members of other libraries, of source files that are not migrated, members that
  are not found and CL `INCLUDE` commands on several lines. The preview shows them.

**TOBi files.** If the build tool of the new project is TOBi, the migration also writes:

* `.ibmi.json` in each directory with the CCSID of the source file (`tgtCcsid`).
* `Rules.mk` in the project directory with the directories (`SUBDIRS`).
* A draft of `Rules.mk` in each directory with one rule per member: a program, or a module, if the
  header of the member uses `CRTRPGMOD`, `CRTCLMOD` or `OBJTYPE(*MODULE)`; `.FILE` for display,
  printer and database files; `.CMD` and `.PNLGRP`. Copy books are prerequisites with their path in
  the project, CL rules set the directory of the source as `INCDIR`, and a source with an
  `EXEC SQL INCLUDE` of a stream file gets `DBGVIEW := *LIST`, because the default `*SOURCE` of
  TOBi fails for it (`SQL8009`). Check the draft and set the
  parameters of the installer or of the `STRPREPRC` headers, because the defaults of TOBi differ
  from the CL commands, e.g. `DBGVIEW` and `ACTGRP`.

**Preview.** The second page lists the members with source type, stream file, status (*New*,
*Error*) and problems. Members without source type are not migrated. Warnings, e.g. a source type
unknown to TOBi, `$` or `#` in the member name or include directives that are not converted, do not
prevent the migration. The tool tip of a row shows all problems; select a member to see its details.
A **double-click on a member** shows the source, that would be written into the stream file, like
the preview of the export: the tab *Result* with line numbers and the search, the tab
*Comparison* with the member and the stream file side by side.

**Project.** The third page is the page of *New > IFS Project...*. It proposes the directory
`<home>/<library>`; a directory with `$` or `#` is not accepted. *Finish* creates the project and
writes the files in the background. Before an existing file is replaced, you are asked: *Yes*, *Yes
To All*, *No*, *No To All* or *Cancel*. A summary is shown at the end.

## Files on the IBM i

The plug-in stores its files in the folder `.ifsprojects` of your home directory:

* `tmp`: temporary files of builds, syntax checks, exports and migrations. They are deleted after
  24 hours.
* `builds/<project>`: default deploy directories of local IFS projects.

## Preferences

*Window > Preferences > IFS Projects*:

* *IFS Projects*: *Run build and compile commands* in the SSH terminal of the project (default) or
  in a job of the RSE connection, see [Where the build runs](#where-the-build-runs), and *Reuse the
  terminal of a project for building*. The defaults of the IFS projects: build and compile command,
  with presets for TOBi (`makei build`, `makei compile -f {filename}`) and GNU Make (`gmake`,
  `gmake {filename}`), and environment variables. A variable of a project overrides the default of
  the same name.
* *SSH Terminal*: connection (port, timeout, keep alive interval, encoding), log-on (user ID in
  lowercase, password of the RSE connection) and *Restore terminals after a restart*.
* *Migration*: *Build tool headers* lists the build tools, whose headers are read when source
  members are migrated, see [Migration to an IFS project](#migration-to-an-ifs-project). All are
  used by default. Switch one off, if its headers are obsolete, e.g. because the members are no
  longer built with it. The list is empty, if no feature of a build tool is installed.
* *Export to Library*: CCSID and record length of new source files for the first export of a
  project. The CCSID is also the default for source files with CCSID 65535 of the migration.

## For developers

How the plug-in works internally and how it is built: [DEVELOPMENT.md](DEVELOPMENT.md).
How to add a build tool with a plug-in of your own: [EXTENDING.md](EXTENDING.md).

## License

Common Public License v1.0, see http://www.eclipse.org/legal/cpl-v10.html.
The icons of the terminal view are taken from the Eclipse TM Terminal (EPL).
