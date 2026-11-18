# Java SE network probe

A Java SE smoke-test client for the controlled local HTTP/TCP/UDP echo server.

Run from the repository root with a JDK and Python 3:

```sh
bash tools/run-java-probe-tests.sh
```

**Scope:** this is not a MIDlet, does not use `javax.microedition.io`, and
does not establish Java ME Generic Connection Framework (GCF) compatibility.
It checks ordinary Java SE `java.net` behavior only. CheerpJ/browser execution
and real MIDlet semantics must be tested separately.
