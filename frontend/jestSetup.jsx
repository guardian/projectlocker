// setup file
import { configure } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
// const {
//     Readable,
//     Writable,
//     Transform,
//     Duplex,
//     pipeline,
//     finished
// } = require('readable-stream');

global.fetch = require('jest-fetch-mock');
//global.stream = require('web-streams-polyfill/ponyfill');
//global.ReadableStream= Readable;

require("web-streams-polyfill/es6");

configure({ adapter: new Adapter() });