
const startMessages: Record<string, string> = {
    suite: 'testSuiteStarted',
    test: 'testStarted',
};

const resultStateMessages: Record<string, string> = {
    fail: 'testFailed',
    skip: 'testIgnored',
};

const endMessages: Record<string, string> = {
    suite: 'testSuiteFinished',
    test: 'testFinished',
};

export {startMessages, resultStateMessages, endMessages};