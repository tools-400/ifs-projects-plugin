# Extending IFS Projects

How to write a plug-in that adds a build tool to IFS Projects. For the use of the plug-in, see
[README.md](README.md), for its internals [DEVELOPMENT.md](DEVELOPMENT.md).

An extension plug-in is an ordinary Eclipse plug-in, that requires
`de.tools400.ifsprojects.core.plugin` and contributes to one of its extension points. It is shipped
as a feature of its own, so that only the build tools, that are used, are installed.

## Extension points

| Extension point | Interface | Purpose |
|---|---|---|
| `de.tools400.ifsprojects.core.plugin.sourceHeaderParsers` | `ISourceHeaderParser` | Reads the compile commands of the header of a build tool, e.g. `STRPREPRC`. |
| `de.tools400.ifsprojects.core.plugin.terminalLaunchers` | `ITerminalLauncher` | Opens the SSH terminals. The available backend with the highest priority is used. |

Both interfaces are in exported packages (`de.tools400.ifsprojects.sources.headers` and
`de.tools400.ifsprojects.terminal`); the rest of the plug-in is internal. The reference
implementation of a header parser is the plug-in `IFS Projects STRPREPRC Plugin` of this
repository.

## A parser for your build tool

The migration of source members to an IFS project (*Migrate to IFS Project...*) reads the header of
each member to learn which object it creates and where the SQL precompiler searches its includes.
Which markers a header uses is specific to the build tool; the CL commands in it are not. **Your
parser only finds the commands, the core evaluates them.**

### 1. The plug-in project

`META-INF/MANIFEST.MF`:

```
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: My Build Tool Headers
Bundle-SymbolicName: com.example.ifsprojects.mybuildtool;singleton:=true
Bundle-Version: 1.0.0
Bundle-RequiredExecutionEnvironment: JavaSE-1.8
Bundle-ActivationPolicy: lazy
Require-Bundle: de.tools400.ifsprojects.core.plugin
Export-Package: com.example.ifsprojects.mybuildtool
```

Java 8 byte code, because IFS Projects runs in RDi 9.6. `Export-Package` is only needed, if your
tests live in a fragment of their own.

### 2. The extension

`plugin.xml`:

```xml
<extension point="de.tools400.ifsprojects.core.plugin.sourceHeaderParsers">
   <parser
         id="com.example.ifsprojects.headers.mybuildtool"
         name="%Parser_MyBuildTool"
         class="com.example.ifsprojects.mybuildtool.MyBuildToolHeaderParser"
         priority="50">
   </parser>
</extension>
```

The parsers are called by their priority, the highest one first; the first parser that finds a
header wins. The `STRPREPRC` parser has priority 100. Because the markers of the build tools
differ, the order rarely matters - give your parser a priority below 100, unless it has to be asked
first.

The `name` is the name of your build tool and is shown on the preference page *IFS Projects >
Migration*, on which a user switches parsers off. It is translatable, so put it into
`plugin.properties` and `plugin_de.properties`. The **`id` is the key, under which the setting is
stored** - keep it stable across versions, otherwise a parser that a user has switched off is
active again after an update.

### 3. The parser

The example reads the `BUILD` command of Scott Klement, whose header lines start with `*>` and are
continued by a trailing `-`:

```java
package com.example.ifsprojects.mybuildtool;

import java.util.ArrayList;
import java.util.List;

import de.tools400.ifsprojects.sources.headers.CreateCommand;
import de.tools400.ifsprojects.sources.headers.ISourceHeaderParser;

public final class MyBuildToolHeaderParser implements ISourceHeaderParser {

    private static final String TAG = "*>";
    private static final int HEADER_LINES = 100;

    public List<CreateCommand> parse(String aContent, String aSourceType) {

        List<CreateCommand> commands = new ArrayList<CreateCommand>();

        String[] lines = aContent.split("\n");
        for (int i = 0; i < lines.length && i < HEADER_LINES; i++) {

            int tag = lines[i].indexOf(TAG);
            if (tag < 0) {
                continue;
            }

            String text = lines[i].substring(tag + TAG.length()).trim();
            while (text.endsWith("-") && i + 1 < lines.length) {
                String next = lines[++i];
                int tagOfNext = next.indexOf(TAG);
                text = text.substring(0, text.length() - 1) + ' '
                    + (tagOfNext < 0 ? next : next.substring(tagOfNext + TAG.length())).trim();
            }

            CreateCommand command = CreateCommand.parse(text);
            if (command != null) {
                commands.add(command);
            }
        }

        return commands;
    }
}
```

`aContent` is the source with the line separator `\n`, `aSourceType` its source type, e.g.
`RPGLE`, or `null`.

### 4. What the core does with the commands

`CreateCommand.parse()` splits one CL command; `SourceHeader` answers what the migration needs:

| Method | Returns |
|---|---|
| `CreateCommand.getName()` | name in upper case without its library, e.g. `CRTSQLRPGI` |
| `CreateCommand.getParameter(name)` | value without its parentheses, e.g. `&SL/QCPYSRC` for `INCFILE` |
| `CreateCommand.isCreateCommand()` | whether the name starts with `CRT` |
| `SourceHeader.getCreateCommand()` | the last command whose name starts with `CRT`: a header that creates modules binds them afterwards, and a `CHGOBJOWN` at the end creates nothing |
| `SourceHeader.isModule()` | `CRT*MOD` or `OBJTYPE(*MODULE)` - the member becomes a `.MODULE` target in the `Rules.mk` draft |
| `SourceHeader.getIncludeFile()` | `INCFILE` without a library that is a variable - the source file of the `EXEC SQL INCLUDE` statements |

Nested parentheses and quoted strings are handled, e.g. `BNDDIR((*LIBL/MAIN) (QC2LE))` and
`TEXT('Order (new)')`.

### 5. Rules

* Return **an empty list**, if the source has no header of your build tool. The next parser is
  asked, and a source that no parser knows simply has no header: the migration then takes the
  object type from the source type and the SQL include file from the migrated source files.
* Return **each command as one line**: comment characters removed, continuation lines joined.
* **Keep the substitution variables** of your build tool in the commands and let them start with
  `&`, e.g. `INCFILE(&SL/QCPYSRC)`, so that the core can tell a library that is a variable from a
  real one.
* **Do not throw.** A runtime exception is caught and logged, but the header is lost.
* **Stop at the source.** Search a limited number of lines and end a command at a line that is no
  comment, so that an unterminated header does not swallow the source.
* **Expect to be switched off.** A user can disable your parser on *IFS Projects > Migration*; the
  migration then runs without it. Nothing else of your plug-in should depend on being asked.

### 6. Tests

A parser is plain Java without Eclipse dependencies, so it can be tested with JUnit alone. The
registry can be tested without the platform, too:

```java
SourceHeaderParsers parsers = new SourceHeaderParsers(new MyBuildToolHeaderParser());
SourceHeader header = parsers.parse(content, "RPGLE");

assertTrue(header.isModule());
assertEquals("QCPYSRC", header.getIncludeFile());
```

`SourceHeaderParsers.getContributions()` reads the extension registry instead, and
`getEnabled()`, which the migration uses, leaves out the parsers that the user has switched off.

### 7. Shipping it

Put the plug-in into a feature of its own, which imports the core feature:

```xml
<requires>
   <import feature="de.tools400.ifsprojects.core.feature" version="1.0.0" match="greaterOrEqual"/>
</requires>
```

A feature is also the unit that the Eclipse Marketplace can mark as optional, so that users pick
the build tools they need. Inside this repository, a new plug-in or feature project additionally
has to be added to `build.projects` of `build/IFS Projects Build/build/build.properties`, to
`site.xml` of the update site and to the launch configurations in
`eclipse/IFS Projects Core Plugin/launches`.
