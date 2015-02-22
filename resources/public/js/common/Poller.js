var IdleDetector = function() {
    return ({
        __listeners: [],
        attachToDocument: function() {
            document.addEventListener('mousemove', this.__onMouseMove.bind(this));
            document.addEventListener('mouseup', this.__onMouseUp.bind(this));
            document.addEventListener('keyup', this.__onKeyUp.bind(this));
            document.addEventListener('keydown', this.__onKeyDown.bind(this));
        },
        detachFromDocument: function() {
            document.removeEventListener('mousemove', this.__onMouseMove.bind(this));
            document.removeEventListener('mouseup', this.__onMouseUp.bind(this));
            document.removeEventListener('keyup', this.__onKeyUp.bind(this));
            document.removeEventListener('keydown', this.__onKeyDown.bind(this));
        },
        addActivityListener: function(listener) {
            this.__listeners.push(listener);
        },
        removeActivityListener: function(listener) {
            var pos = this.__listeners.indexOf(listener);
            if (pos != -1)
                this.__listeners.remove(pos);
        },
        __onMouseMove: function() {
            this.__saveActivityTime();
        },
        __onMouseUp: function() {
            this.__saveActivityTime();
        },
        __onKeyUp: function() {
            this.__saveActivityTime();
        },
        __onKeyDown: function() {
            this.__saveActivityTime();
        },
        __saveActivityTime: function() {
            var now = new Date();
            this.__lastActivityTime = now;

            var needFireEvent = !this.__fireActivityEventTime || Math.abs(now - this.__fireActivityEventTime) > 5000;
            this.__fireActivityEventTime = now;

            if (needFireEvent) {
                for (var i = 0; i < this.__listeners.length; ++i) {
                    this.__listeners[i]();
                }
            }
        },
        isIdle: function() {
            if (!this.__lastActivityTime)
                return false;
            return Math.abs(new Date() - this.__lastActivityTime) > 60000;
        }
    });
};

var BasedOnActivityIntervalTimer = function(func, activityTimeout, idleTimeout, idleDetector) {
    var detector = ({
        __start: function(func, timeout, idleTimeout, idleDetector) {
            idleDetector.addActivityListener(this.__installTimer.bind(this));
            this.__idleDetector = idleDetector;
            this.__activityTimeout = activityTimeout;
            this.__idleTimeout = idleTimeout;
            this.__func = func;
            this.__installTimer();
            func();
        },
        __installTimer: function() {
            if (this.__destroyed)
                return;
            var timeout = this.__idleDetector.isIdle() ? this.__idleTimeout : this.__activityTimeout;
            if (this.__currentTimeout && this.__currentTimeout != timeout)
                this.__uninstallTimer();
            if (!this.__interval) {
                this.__currentTimeout = timeout;
                this.__interval = setInterval(function() {
                    this.__func();
                    this.__installTimer();
                }.bind(this), timeout);
            }
        },
        stop: function() {
            this.__idleDetector.removeActivityListener(this.__installTimer.bind(this));
            this.__uninstallTimer();
        },
        __uninstallTimer: function() {
            this.__destroyed = true;
            if (this.__interval) {
                clearInterval(this.__interval);
                delete this.__interval;
                delete this.__currentTimeout;
            }
        }
    });
    detector.__start(func, activityTimeout, idleTimeout, idleDetector);
    return detector;
};
