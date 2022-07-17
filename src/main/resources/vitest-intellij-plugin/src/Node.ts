import { endMessages, resultStateMessages, startMessages } from './constants';
import { teamCityMessage } from './utils';
import type { File, TaskResult } from 'vitest';

/**
 * @class
 * @constructor
 * @public
 */
export default class Node {
  private readonly _id: string;
  private readonly nodeId: string;
  private readonly name: string;
  private readonly file: File | undefined;
  private readonly _parentNode: Node | null;
  private readonly parentNodeId: string | null;
  private readonly _type: string;
  private readonly startMessage: string;
  private readonly endMessage: string;
  private readonly children: Record<string, Node>;
  private childCount: number;
  private childExecutionCount: number;
  constructor(
    type: string,
    id: string,
    name: string,
    file: File | undefined,
    parentNode: Node | null = null
  ) {
    /**
     * @type {string}
     * @public
     */
    this._id = id;
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
     * @type {File|undefined}
     * @private
     */
    this.file = file;
    /**
     * @type {Node}
     * @public
     */
    this._parentNode = parentNode;
    /**
     * @type {string}
     * @public
     */
    this.parentNodeId = parentNode === null ? null : parentNode._id;
    /**
     * @public
     */
    this._type = type;
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

  get id(): string {
    return this._id;
  }

  get type(): string {
    return this._type;
  }

  get parentNode(): Node | null {
    return this._parentNode;
  }

  createStartMessage() {
    if (this.startMessage) {
      return teamCityMessage(this.startMessage, {
        id: this._id,
        name: this.name,
        nodeId: this.nodeId,
        parentNodeId: this.parentNodeId,
      });
    }
    return null;
  }

  createResultMessage(result?: TaskResult) {
    if (result) {
      const { state, error } = result;
      if (state in resultStateMessages) {
        return teamCityMessage(resultStateMessages[state], {
          id: this._id,
          name: this.name,
          nodeId: this.nodeId,
          parentNodeId: this.parentNodeId,
          ...(this.file?.filepath
            ? { locationHint: `file::/${this.file.filepath}` }
            : {}),
          ...(error?.message ? { message: error.message } : {}),
        });
      }
    }
    return null;
  }

  createEndMessage(duration?: number | null) {
    if (this.endMessage) {
      return teamCityMessage(this.endMessage, {
        id: this._id,
        name: this.name,
        nodeId: this.nodeId,
        parentNodeId: this.parentNodeId,
        ...(duration ? { duration: duration.toString() } : {}),
      });
    }
    return null;
  }

  addChild(node: Node) {
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
