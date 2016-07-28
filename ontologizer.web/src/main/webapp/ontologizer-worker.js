/**
 * The Ontologizer Worker
 */

window = new Object()

importScripts('teavm/runtime.js', 'teavm/classes.js');

console.log("Script Worker!");

self.addEventListener('message', function(e) {
		console.log(e.data);
	}, false);
