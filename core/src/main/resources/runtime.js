"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var runtime;
(function (runtime) {
    var math;
    (function (math) {
        function floor(thisref, p1) {
            return Math.floor(p1);
        }
        function ceil(thisref, p1) {
            return Math.ceil(p1);
        }
        function sin(thisref, p1) {
            return Math.sin(p1);
        }
        function cos(thisref, p1) {
            return Math.cos(p1);
        }
        function round(thisref, p1) {
            return Math.round(p1);
        }
        function float_rem(a, b) {
            return a % b;
        }
        function sqrt(thisref, p1) {
            return Math.sqrt(p1);
        }
    })(math || (math = {}));
})(runtime = exports.runtime || (exports.runtime = {}));
