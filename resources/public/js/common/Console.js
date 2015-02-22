var Console = {
    log: function(message) {
        if (typeof console !== undefined)
            console.log('[' + new Date().toString() + ']: ' + message);
    }
};
