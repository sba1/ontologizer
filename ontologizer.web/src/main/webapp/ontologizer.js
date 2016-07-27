/**
 * Main Ontologizer Web Javascript file
 */

function main() {
	var worker = new Worker('ontologizer-worker.js');
	worker.postMessage('Start');
}
