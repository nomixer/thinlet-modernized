# Manual caret probe (scratch — not built by Maven, not for merge)

Investigates the one deferred input path from **D40**: does a real **mouse click**
reposition the text caret? The headless synthetic `InputDriver` can't reproduce it
(a bare synthetic press doesn't prime the field's `:offset`/`referencex`), so this
runs Thinlet with real AWT on your desktop and prints the caret on every change.

## Run it (on a machine with a real display — NOT via surefire, which forces `DISPLAY=:99`)

```sh
# 1. get this branch
git fetch origin manual/caret-probe
git checkout manual/caret-probe          # or: git worktree add ../thinlet-caret manual/caret-probe

# 2. build thinlet-core's classes once (skips tests + lint)
./mvnw -q -pl thinlet-core -DskipTests package

# 3. compile the probe against those classes
javac -cp thinlet-core/target/classes -d /tmp/caret-probe manual-test/CaretProbe.java

# 4. run it on your real display
java -cp thinlet-core/target/classes:/tmp/caret-probe CaretProbe manual-test/caret.xml
```

A window opens. In the **textfield** (pre-filled `hello world`) and the **textarea**:
type something, then **click in the middle of the text**. Each caret change prints:

```
tf  start=<anchor> end=<caret>  text="hello world"
```

## What to report back

- After clicking mid-text, does `end` (the caret) jump to the character you clicked,
  or stay put? (Report a couple of click positions + the printed `start`/`end`.)
- Same for the textarea (multi-line).

If clicking **does** move the caret on the real desktop, the synthetic driver has a
fidelity gap to fix (prime `referencex`/`:offset` on press). If it **doesn't**, the
behavior is as-is and no synthetic click-caret test is warranted.

## Optional bonus: Robot cross-check with a real window manager

The `@Tag("robot")` fidelity cross-check (`InputRobotFidelityTest`, D40) runs WM-less on
Xvfb `:99` in CI. On a real desktop you can confirm it holds with real focus/activation:

```sh
./mvnw -pl thinlet-core test -Dtest=InputRobotFidelityTest
```

(This one *does* go through surefire, but Robot drives whatever display the fork uses, so
on your desktop it exercises native focus/activation under your window manager.)

