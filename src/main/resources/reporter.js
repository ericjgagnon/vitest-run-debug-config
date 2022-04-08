/**
 * @format
 */
const doEscapeCharCode = (function () {
  const obj = {};

  function addMapping(fromChar, toChar) {
    if (fromChar.length !== 1 || toChar.length !== 1) {
      throw Error('String length should be 1');
    }
    const fromCharCode = fromChar.charCodeAt(0);
    if (typeof obj[fromCharCode] === 'undefined') {
      obj[fromCharCode] = toChar;
    } else {
      throw Error('Bad mapping');
    }
  }

  addMapping('\n', 'n');
  addMapping('\r', 'r');
  addMapping('\u0085', 'x');
  addMapping('\u2028', 'l');
  addMapping('\u2029', 'p');
  addMapping('|', '|');
  addMapping("'", "'");
  addMapping('[', '[');
  addMapping(']', ']');

  return function (charCode) {
    return obj[charCode];
  };
})();

function isAttributeValueEscapingNeeded(str) {
  const len = str.length;
  for (let i = 0; i < len; i++) {
    if (doEscapeCharCode(str.charCodeAt(i))) {
      return true;
    }
  }
  return false;
}

function escape(str) {
  if (!isAttributeValueEscapingNeeded(str)) {
    return str;
  }
  let res = '';
  const len = str.length;
  for (let i = 0; i < len; i++) {
    const escaped = doEscapeCharCode(str.charCodeAt(i));
    if (escaped) {
      res += '|';
      res += escaped;
    } else {
      res += str.charAt(i);
    }
  }
  return res;
}

function teamCityMessage(type, params = {}) {
  const strParams = Object.entries(params)
      .reduce((acc, [name, value]) => {
        if (value !== null && value !== undefined) {
          acc.push(`${name}='${escape(value)}'`);
        }
        return acc;
      }, [])
      .join(' ');

  if (strParams) {
    return `##teamcity[${type} ${strParams}]`;
  }
  return `##teamcity[${type}]`;
}

function writeToStdOut(message) {
  if (message === null || message === undefined) {
    return;
  }

  let writableMsg = message;
  if (typeof message !== 'string') {
    writableMsg = JSON.stringify(message, null, 2);
  }
  process.stdout.write(writableMsg);
  process.stdout.write('\n');
}

function writeToStdErr(message) {
  process.stderr.write(message);
}

const startMessages = {
  suite: 'testSuiteStarted',
  test: 'testStarted',
};

const resultStateMessages = {
  fail: 'testFailed',
  skip: 'testIgnored',
};

const endMessages = {
  suite: 'testSuiteFinished',
  test: 'testFinished',
};

/**
 * @class
 * @constructor
 * @public
 */
class Node {
  constructor(type, id, name, parentNode = null) {
    /**
     * @type {string}
     * @public
     */
    this.id = id;
    /**
     * @type {string}
     * @private
     */
    this.nodeId = id;
    /**
     * @type {string}
     * @public
     */
    this.name = name;
    /**
     * @type {string}
     * @public
     */
    this.state = 'pending';
    /**
     * @type {Node}
     * @public
     */
    this.parentNode = parentNode;
    /**
     * @type {string}
     * @public
     */
    this.parentNodeId = parentNode ? parentNode.id : null;
    /**
     * @type {string}
     * @public
     */
    this.type = type;
    /**
     * @type {string|undefined}
     * @private
     */
    this.startMessage = startMessages[type];
    /**
     * @type {string|undefined}
     * @private
     */
    this.endMessage = endMessages[type];
    /**
     * @type {Record<string, Node>}
     * @private
     */
    this.children = {};
    /**
     * @type {number}
     * @public
     */
    this.childCount = 0;
    /**
     * @type {number}
     * @public
     */
    this.childExecutionCount = 0;
  }

  createStartMessage() {
    if (this.startMessage) {
      return teamCityMessage(this.startMessage, {
        id: this.id,
        name: this.name,
        nodeId: this.nodeId,
        parentNodeId: this.parentNodeId,
      });
    }
    return null;
  }

  createResultMessage(result) {
    const { state, error } = result;
    if (state in resultStateMessages) {
      return teamCityMessage(resultStateMessages[state], {
        id: this.id,
        name: this.name,
        nodeId: this.nodeId,
        parentNodeId: this.parentNodeId,
        ...(error?.message ? { message: error.message } : {}),
      });
    }
    return null;
  }

  createEndMessage(duration) {
    if (this.endMessage) {
      return teamCityMessage(this.endMessage, {
        id: this.id,
        name: this.name,
        nodeId: this.nodeId,
        parentNodeId: this.parentNodeId,
        ...(duration ? { duration } : {}),

      });
    }
    return null;
  }

  addChild(node) {
    this.children[node.id] = node;
    this.childCount++;
  }

  incrementExecutedChildCount() {
    this.childExecutionCount++;
  }

  hasRanAllTests() {
    return this.childExecutionCount === this.childCount;
  }
}

module.exports = class IntellijTestReporter {
  /**
   *
   * @type {Node[]}
   */
  testPlan = [];
  /**
   *
   * @type {Record<string, Node>}
   */
  testPlanLookup = {};

  onInit() {
    writeToStdOut(teamCityMessage('testingStarted'));
  }

  addToPlan(node) {
    this.testPlan.push(node);
    this.testPlanLookup[node.id] = node;
  }

  depthTraversal(tasks = [], messageExtractor = () => {}, parentNode) {
    tasks.forEach((task) => {
      const node = new Node(
          task.type,
          task.id,
          task.name,
          parentNode ? parentNode : null,
      );
      if (parentNode) {
        parentNode.addChild(node);
      }
      this.addToPlan(node);
      this.depthTraversal(task.tasks, messageExtractor, node);
    });
  }

  onCollected(files) {
    (files || []).forEach((file) => {
      this.depthTraversal(
          file.tasks,
          (node) => node.createStartMessage(),
          null,
      );
    });
  }

  /**
   *
   * @param {Array<{}>} files
   */
  onFinished(files) {
    writeToStdOut(teamCityMessage('testingFinished'));
  }

  /**
   *
   * @param packs
   */
  onTaskUpdate(packs) {
    (packs || []).forEach(([id, result]) => {
      /**
       *
       * @type {Node}
       */
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
            writeToStdOut(node.createEndMessage(result.duration));
            let suite = node.parentNode;
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
  onWatcherStart() {
    console.log(`onWatcherStart`);
  }
  onWatcherRerun(files, trigger) {
    console.log(`onWatcherRun`, files, trigger);
  }
  onServerRestart() {
    console.log('onServerRestart');
  }
  onUserConsoleLog(log) {
    console.log('onUserConsoleLog', log);
  }
};
