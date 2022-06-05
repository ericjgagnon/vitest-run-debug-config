import { EOL } from 'os';

const doEscapeCharCode = (function () {
  const obj: Record<string, string | undefined> = {};

  function addMapping(fromChar: string, toChar: string) {
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

  return function (charCode: string | number) {
    return obj[charCode];
  };
})();

function isAttributeValueEscapingNeeded(str: string) {
  const len = str.length;
  for (let i = 0; i < len; i++) {
    if (doEscapeCharCode(str.charCodeAt(i))) {
      return true;
    }
  }
  return false;
}

function escape(str: string) {
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

function teamCityMessage(
  type: string,
  params: Record<string, string | null> = {}
) {
  const strParams = Object.entries(params)
    .reduce((acc, [name, value]) => {
      if (value !== null && value !== undefined) {
        acc.push(`${name}='${escape(value)}'`);
      }
      return acc;
    }, [] as string[])
    .join(' ');

  if (strParams) {
    return `##teamcity[${type} ${strParams}]`;
  }
  return `##teamcity[${type}]`;
}

function writeToStdOut(message?: string | null) {
  if (message === null || message === undefined) {
    return;
  }

  process.stdout.write(message);
  process.stdout.write(EOL);
}
export { teamCityMessage, writeToStdOut };
