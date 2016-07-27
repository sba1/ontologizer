/**
 * The Ontologizer Worker
 */

importScripts();

console.log("Worker!");

self.addEventListener('message', function(e) {
		console.log(e.data);
	}, false);
