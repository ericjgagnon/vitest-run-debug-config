var __defProp = Object.defineProperty;
var __getOwnPropSymbols = Object.getOwnPropertySymbols;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __propIsEnum = Object.prototype.propertyIsEnumerable;
var __defNormalProp = (obj, key, value) => key in obj ? __defProp(obj, key, { enumerable: true, configurable: true, writable: true, value }) : obj[key] = value;
var __spreadValues = (a, b) => {
  for (var prop in b || (b = {}))
    if (__hasOwnProp.call(b, prop))
      __defNormalProp(a, prop, b[prop]);
  if (__getOwnPropSymbols)
    for (var prop of __getOwnPropSymbols(b)) {
      if (__propIsEnum.call(b, prop))
        __defNormalProp(a, prop, b[prop]);
    }
  return a;
};
var __publicField = (obj, key, value) => {
  __defNormalProp(obj, typeof key !== "symbol" ? key + "" : key, value);
  return value;
};
var __accessCheck = (obj, member, msg) => {
  if (!member.has(obj))
    throw TypeError("Cannot " + msg);
};
var __privateAdd = (obj, member, value) => {
  if (member.has(obj))
    throw TypeError("Cannot add the same private member more than once");
  member instanceof WeakSet ? member.add(obj) : member.set(obj, value);
};
var __privateMethod = (obj, member, method) => {
  __accessCheck(obj, member, "access private method");
  return method;
};
var _depthTraversal, depthTraversal_fn;
const startMessages = {
  suite: "testSuiteStarted",
  test: "testStarted"
};
const resultStateMessages = {
  fail: "testFailed",
  skip: "testIgnored"
};
const endMessages = {
  suite: "testSuiteFinished",
  test: "testFinished"
};
const EOL = "\n";
const doEscapeCharCode = function() {
  const obj = {};
  function addMapping(fromChar, toChar) {
    if (fromChar.length !== 1 || toChar.length !== 1) {
      throw Error("String length should be 1");
    }
    const fromCharCode = fromChar.charCodeAt(0);
    if (typeof obj[fromCharCode] === "undefined") {
      obj[fromCharCode] = toChar;
    } else {
      throw Error("Bad mapping");
    }
  }
  addMapping("\n", "n");
  addMapping("\r", "r");
  addMapping("\x85", "x");
  addMapping("\u2028", "l");
  addMapping("\u2029", "p");
  addMapping("|", "|");
  addMapping("'", "'");
  addMapping("[", "[");
  addMapping("]", "]");
  return function(charCode) {
    return obj[charCode];
  };
}();
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
  let res = "";
  const len = str.length;
  for (let i = 0; i < len; i++) {
    const escaped = doEscapeCharCode(str.charCodeAt(i));
    if (escaped) {
      res += "|";
      res += escaped;
    } else {
      res += str.charAt(i);
    }
  }
  return res;
}
function teamCityMessage(type, params = {}) {
  const strParams = Object.entries(params).reduce((acc, [name, value]) => {
    if (value !== null && value !== void 0) {
      acc.push(`${name}='${escape(value)}'`);
    }
    return acc;
  }, []).join(" ");
  if (strParams) {
    return `##teamcity[${type} ${strParams}]`;
  }
  return `##teamcity[${type}]`;
}
function writeToStdOut(message) {
  if (message === null || message === void 0) {
    return;
  }
  process.stdout.write(message);
  process.stdout.write(EOL);
}
class Node {
  constructor(type, id, name, file, parentNode = null) {
    __publicField(this, "_id");
    __publicField(this, "nodeId");
    __publicField(this, "name");
    __publicField(this, "file");
    __publicField(this, "_parentNode");
    __publicField(this, "parentNodeId");
    __publicField(this, "_type");
    __publicField(this, "startMessage");
    __publicField(this, "endMessage");
    __publicField(this, "children");
    __publicField(this, "childCount");
    __publicField(this, "childExecutionCount");
    this._id = id;
    this.nodeId = id;
    this.name = name;
    this.file = file;
    this._parentNode = parentNode;
    this.parentNodeId = parentNode === null ? null : parentNode._id;
    this._type = type;
    this.startMessage = startMessages[type];
    this.endMessage = endMessages[type];
    this.children = {};
    this.childCount = 0;
    this.childExecutionCount = 0;
  }
  get id() {
    return this._id;
  }
  get type() {
    return this._type;
  }
  get parentNode() {
    return this._parentNode;
  }
  createStartMessage() {
    if (this.startMessage) {
      return teamCityMessage(this.startMessage, {
        id: this._id,
        name: this.name,
        nodeId: this.nodeId,
        parentNodeId: this.parentNodeId
      });
    }
    return null;
  }
  createResultMessage(result) {
    var _a;
    if (result) {
      const { state, error } = result;
      if (state in resultStateMessages) {
        return teamCityMessage(resultStateMessages[state], __spreadValues(__spreadValues({
          id: this._id,
          name: this.name,
          nodeId: this.nodeId,
          parentNodeId: this.parentNodeId
        }, ((_a = this.file) == null ? void 0 : _a.filepath) ? { locationHint: `file::/${this.file.filepath}` } : {}), (error == null ? void 0 : error.message) ? { message: error.message } : {}));
      }
    }
    return null;
  }
  createEndMessage(duration) {
    if (this.endMessage) {
      return teamCityMessage(this.endMessage, __spreadValues({
        id: this._id,
        name: this.name,
        nodeId: this.nodeId,
        parentNodeId: this.parentNodeId
      }, duration ? { duration: duration.toString() } : {}));
    }
    return null;
  }
  addChild(node) {
    this.children[node._id] = node;
    this.childCount++;
  }
  incrementExecutedChildCount() {
    this.childExecutionCount++;
  }
  hasRanAllTests() {
    return this.childExecutionCount === this.childCount;
  }
}
class IntellijTestReporter {
  constructor() {
    __privateAdd(this, _depthTraversal);
    __publicField(this, "testPlanLookup", {});
  }
  onInit() {
    writeToStdOut(teamCityMessage("testingStarted"));
  }
  addToPlan(node) {
    this.testPlanLookup[node.id] = node;
  }
  onCollected(files) {
    (files || []).forEach((file) => {
      __privateMethod(this, _depthTraversal, depthTraversal_fn).call(this, file.tasks, (node) => node.createStartMessage(), null);
    });
  }
  onFinished() {
    writeToStdOut(teamCityMessage("testingFinished"));
  }
  onTaskUpdate(packs) {
    (packs || []).forEach(([id, result]) => {
      var _a;
      const node = this.testPlanLookup[id];
      if (node) {
        switch (node.type) {
          case "suite": {
            writeToStdOut(node.createStartMessage());
            break;
          }
          case "test": {
            writeToStdOut(node.createStartMessage());
            writeToStdOut(node.createResultMessage(result));
            writeToStdOut(node.createEndMessage((_a = result == null ? void 0 : result.duration) != null ? _a : 0));
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
_depthTraversal = new WeakSet();
depthTraversal_fn = function(tasks = [], messageExtractor = () => null, parentNode) {
  tasks.forEach((task) => {
    const node = new Node(task.type, task.id, task.name, task.file, parentNode ? parentNode : null);
    if (parentNode) {
      parentNode.addChild(node);
    }
    this.addToPlan(node);
    if (task.type === "suite") {
      __privateMethod(this, _depthTraversal, depthTraversal_fn).call(this, task.tasks, messageExtractor, node);
    }
  });
};
export { IntellijTestReporter as default };
