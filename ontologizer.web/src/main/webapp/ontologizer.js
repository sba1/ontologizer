/**
 * Main Ontologizer Web Javascript file
 */

function main() {
	var worker = new Worker('ontologizer-worker.js');
	worker.postMessage({type: 'ontologizer.WorkerMessage', msg: 'Hello, worker!'});
	worker.addEventListener('message', function(e)
	{
		console.log('worker sent: ', e.data);
	}, false);
}
