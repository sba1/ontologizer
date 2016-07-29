/**
 * The Ontologizer Worker
 */

/* The TeaVM runtime refers to the global browser context sometimes */
window = self

/* We are using worker to access the worker */
worker = self

importScripts('teavm/runtime.js', 'teavm/classes.js');

console.log("Script Worker!");

main();
