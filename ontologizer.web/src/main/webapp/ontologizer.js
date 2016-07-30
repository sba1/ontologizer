/**
 * Main Ontologizer Web Javascript file
 */

function main() {
	var worker = new Worker('ontologizer-worker.js');
	worker.postMessage('Hello, worker!');
	worker.addEventListener('message', function(e)
	{
		console.log('worker sent: ', e.data);
	}, false);
}
