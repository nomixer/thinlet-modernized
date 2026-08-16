#!/bin/bash
# loop-modernise.sh - Autonomous test-driven Java modernisation loop for Claude Code
#
# NOT YET ADAPTED TO THIS REPOSITORY. Imported as a starting point; see the
# import commit message for what is wrong with it.
#
# Usage: scripts/loop-modernise.sh [java-version] [--new]
#   java-version  Target Java version to modernise towards (default: 8)
#   --new         Clear Claude's context every iteration instead of continuing
#                 the same session

# Configuration
TARGET_VERSION=8
TEST_COMMAND="mvn clean test"
TEST_LOG=".modernise-test.log"
MAX_ITERATIONS=20
COMPACT_INTERVAL=5
ITERATION=0
USE_NEW_SESSION=false
FIRST_RUN=true

# Parse arguments: --new toggles fresh context, first numeric argument is the target
for arg in "$@"; do
    if [ "$arg" == "--new" ]; then
        USE_NEW_SESSION=true
    elif [[ "$arg" =~ ^[0-9]+$ ]]; then
        TARGET_VERSION="$arg"
    fi
done

# Non-interactive run; file edits are applied automatically, other tools still prompt
CLAUDE_FLAGS=(--print --permission-mode acceptEdits)

# Run Claude with the right session mode for this iteration
run_claude() {
    if [ "$USE_NEW_SESSION" = true ] || [ "$FIRST_RUN" = true ]; then
        FIRST_RUN=false
        claude "${CLAUDE_FLAGS[@]}" "$1"
    else
        claude "${CLAUDE_FLAGS[@]}" --continue "$1"
    fi
}

if [ "$USE_NEW_SESSION" = true ]; then
    echo "⚠️  [Mode] Running with '--new' flag. Context will be completely cleared every iteration."
fi

echo "=================================================="
echo "Starting autonomous upgrade loop to Java $TARGET_VERSION..."
echo "=================================================="

PROMPT_TEXT="Review the Java codebase. Identify blocks of code that do not use Java $TARGET_VERSION idioms (e.g., lambdas, streams, var, switch expressions). Modernise one single logical module or class, apply the changes, and ensure the code compiles. Do not change business logic. Write a 1-line summary of what you changed to .git_commit_msg.txt"

while [ $ITERATION -lt $MAX_ITERATIONS ]; do
    CURRENT_STEP=$((ITERATION + 1))
    echo "=================================================="
    echo "Starting Iteration $CURRENT_STEP of $MAX_ITERATIONS..."
    echo "=================================================="

    # 1. Reset the commit message scratchpad
    rm -f .git_commit_msg.txt

    # 2. Ask Claude for the next modernisation step
    run_claude "$PROMPT_TEXT"

    # 3. Guard: check if any modifications actually occurred
    if git diff --quiet; then
        echo "🎉 No changes detected by Git. Modernisation complete or no matching patterns found!"
        break
    fi

    # 4. Verification: run the Java test suite, keeping the output for diagnosis
    echo "⚙️  Running verification test suite ($TEST_COMMAND)..."
    $TEST_COMMAND 2>&1 | tee "$TEST_LOG"
    TEST_RESULT=${PIPESTATUS[0]}

    if [ $TEST_RESULT -eq 0 ]; then
        echo "✅ Tests passed! Processing automated commit..."

        # 5. Extract Claude's custom message, fallback if missing
        if [ -f .git_commit_msg.txt ] && [ -s .git_commit_msg.txt ]; then
            COMMIT_MSG=$(cat .git_commit_msg.txt)
        else
            COMMIT_MSG="modernise: automated incremental Java $TARGET_VERSION upgrade (iteration $CURRENT_STEP)"
        fi

        # 6. Finalise Git tracking
        rm -f .git_commit_msg.txt "$TEST_LOG"
        git add .
        git commit -m "$COMMIT_MSG"

        # 7. Tag the successful milestone
        git tag -a "modernise-java${TARGET_VERSION}-step${CURRENT_STEP}" \
            -m "Autonomous success step ${CURRENT_STEP}"

        ((ITERATION++))

        # 8. Auto-compaction phase (only if not using a fresh session every time)
        if [ "$USE_NEW_SESSION" = false ] && [ $((ITERATION % COMPACT_INTERVAL)) -eq 0 ]; then
            echo "🧹 [Context Management] Compacting session history to prevent token bloat..."
            run_claude "/compact Focus the summary entirely on which Java packages or classes have already been successfully refactored to modern patterns."
        fi
    else
        echo "❌ Tests failed with exit code $TEST_RESULT! The refactoring broke compilation or validation rules."
        echo "🔄 Rolling back to HEAD before attempting a repair..."
        cp "$TEST_LOG" "${TEST_LOG}.failed" 2>/dev/null
        git reset --hard HEAD
        rm -f .git_commit_msg.txt

        echo "🛠️  Feeding the failure log to Claude for one targeted repair attempt..."
        REPAIR_PROMPT="The previous modernisation attempt failed the test suite ($TEST_COMMAND) with exit code $TEST_RESULT and has already been rolled back with 'git reset --hard HEAD', so the working tree is clean again. The captured failure output is in ${TEST_LOG}.failed. Read it, work out which change broke the build, then redo that modernisation to Java $TARGET_VERSION correctly and conservatively, verifying with '$TEST_COMMAND'. If you cannot fix it safely, make zero changes. Write a 1-line summary to .git_commit_msg.txt"
        run_claude "$REPAIR_PROMPT"

        # Verify whether the repair actually fixed it
        $TEST_COMMAND 2>&1 | tee "$TEST_LOG"
        REPAIR_RESULT=${PIPESTATUS[0]}

        if [ $REPAIR_RESULT -eq 0 ]; then
            echo "✅ Repair successful! Committing..."

            if [ -f .git_commit_msg.txt ] && [ -s .git_commit_msg.txt ]; then
                COMMIT_MSG=$(cat .git_commit_msg.txt)
            else
                COMMIT_MSG="modernise: repaired broken build during Java $TARGET_VERSION transition (iteration $CURRENT_STEP)"
            fi

            rm -f .git_commit_msg.txt "$TEST_LOG" "${TEST_LOG}.failed"
            git add .
            git commit -m "$COMMIT_MSG"

            git tag -a "modernise-java${TARGET_VERSION}-step${CURRENT_STEP}" \
                -m "Autonomous repair step ${CURRENT_STEP}"

            ((ITERATION++))
        else
            echo "💥 Repair failed. Hard rollback executed. Exiting loop to prevent corruption."
            git reset --hard HEAD
            rm -f .git_commit_msg.txt "$TEST_LOG"
            exit 1
        fi
    fi
done

echo "=================================================="
echo "Refactoring loop terminated gracefully after $ITERATION iterations."
echo "=================================================="
