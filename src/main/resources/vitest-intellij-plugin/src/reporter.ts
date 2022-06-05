import Node from './Node';
import { teamCityMessage, writeToStdOut } from './utils';
import type { File, Reporter, Task, TaskResultPack } from 'vitest';

export default class IntellijTestReporter implements Reporter {
  testPlanLookup: Record<string, Node> = {};

  onInit() {
    writeToStdOut(teamCityMessage('testingStarted'));
  }

  addToPlan(node: Node) {
    this.testPlanLookup[node.id] = node;
  }

  #depthTraversal(
    tasks: Task[] = [],
    messageExtractor: (node: Node) => string | null = () => null,
    parentNode: Node | null
  ) {
    tasks.forEach((task) => {
      const node = new Node(
        task.type,
        task.id,
        task.name,
        parentNode ? parentNode : null
      );
      if (parentNode) {
        parentNode.addChild(node);
      }
      this.addToPlan(node);

      if (task.type === 'suite') {
        this.#depthTraversal(task.tasks, messageExtractor, node);
      }
    });
  }

  onCollected(files?: File[]) {
    (files || []).forEach((file) => {
      this.#depthTraversal(
        file.tasks,
        (node: Node) => node.createStartMessage(),
        null
      );
    });
  }

  onFinished() {
    writeToStdOut(teamCityMessage('testingFinished'));
  }

  onTaskUpdate(packs: TaskResultPack[]) {
    (packs || []).forEach(([id, result]) => {
      const node = this.testPlanLookup[id];
      if (node) {
        switch (node.type) {
          case 'suite': {
            writeToStdOut(node.createStartMessage());
            break;
          }
          case 'test': {
            writeToStdOut(node.createStartMessage());
            writeToStdOut(node.createResultMessage(result));
            writeToStdOut(node.createEndMessage(result?.duration ?? 0));
            const suite = node.parentNode;
            if (suite) {
              suite.incrementExecutedChildCount();
              if (suite.hasRanAllTests()) {
                writeToStdOut(suite.createEndMessage());
                let parentSuite = suite.parentNode;
                while (parentSuite !== null) {
                  parentSuite.incrementExecutedChildCount();
                  if (parentSuite.hasRanAllTests()) {
                    writeToStdOut(parentSuite.createEndMessage());
                  }
                  parentSuite = parentSuite.parentNode;
                }
              }
            }
            break;
          }
        }
      }
    });
  }
}
