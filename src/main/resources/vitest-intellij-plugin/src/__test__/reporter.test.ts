import { vi } from 'vitest';
import type { File, Suite, TaskResult, TestContext } from 'vitest';
import VitestIntellijReporter from '../reporter';

const reporter = new VitestIntellijReporter();

describe('VitestIntellijReporter', () => {
  const originalWrite = process.stdout.write;
  const mockWriter = vi.fn();

  beforeAll(() => {
    process.stdout.write = mockWriter;
  });

  afterEach(() => {
    mockWriter.mockClear();
  });

  afterAll(() => {
    process.stdout.write = originalWrite;
  });

  test('one file, suite and test', () => {
    let messages = '';
    mockWriter.mockImplementation((message: string) => {
      messages += message;
    });
    reporter.onInit();

    const emptySuite = {} as Suite;
    const emptyContext = {} as TestContext;

    const suite: Suite = {
      id: '1_1',
      type: 'suite',
      name: 'Utils',
      mode: 'run',
      tasks: [
        {
          id: '1_1_1',
          type: 'test',
          name: 'utils.apply',
          mode: 'run',
          suite: emptySuite,
          context: emptyContext,
        },
      ],
    };
    const tasks: File[] = [
      {
        id: '1',
        name: 'utils.test.js',
        type: 'suite',
        mode: 'run',
        filepath: 'path/to/utils.test.js',
        tasks: [suite],
      },
    ];

    const taskResult: TaskResult = {
      state: 'pass',
      duration: 1,
    };

    reporter.onCollected(tasks);
    reporter.onTaskUpdate([
      ['1_1', taskResult],
      ['1_1_1', taskResult],
    ]);
    reporter.onFinished();
    expect(messages).toBe(
      '##teamcity[testingStarted]\n' +
        "##teamcity[testSuiteStarted id='1_1' name='Utils' nodeId='1_1']\n" +
        "##teamcity[testStarted id='1_1_1' name='utils.apply' nodeId='1_1_1' parentNodeId='1_1']\n" +
        "##teamcity[testFinished id='1_1_1' name='utils.apply' nodeId='1_1_1' parentNodeId='1_1' duration='1']\n" +
        "##teamcity[testSuiteFinished id='1_1' name='Utils' nodeId='1_1']\n" +
        '##teamcity[testingFinished]\n'
    );
  });
  test('some test', () => {
    expect(1).toBe(1);
  });
});
