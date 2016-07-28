/**
 * The Ontologizer Worker
 */

/* The TeaVM runtime refers to the global browser context sometimes */
window = self

importScripts('teavm/runtime.js', 'teavm/classes.js');

console.log("Script Worker!");

self.addEventListener('message', function(e) {
		console.log(e.data);
	}, false);
