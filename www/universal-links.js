/**
 * This is the JavaScript interface for the Universal Links plugin.
 * It provides a way to subscribe to events when the app is opened via a universal link.
 */
var exec = require('cordova/exec');

var universalLinks = {
    /**
     * Subscribes to the universal link event.
     *
     * @param {Function} callback - The function to call when a universal link is opened.
     * This function will be passed the URL as a string.
     */
    subscribe: function(callback) {
        if (typeof callback !== 'function') {
            console.error('UniversalLinks.subscribe callback is not a function');
            return;
        }
        exec(callback, null, 'UniversalLinks', 'subscribe', []);
    }
};

module.exports = universalLinks;
