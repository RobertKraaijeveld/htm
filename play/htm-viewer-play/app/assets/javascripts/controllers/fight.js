"use strict";

var FightCtrl = function($scope, $timeout, playRoutes, stateService) {
	_.extend($scope, stateService.get("fight"));
	$scope.totalScore = {a: 0, b: 0, d: 0};
	
	$scope.updateScore = function() {
		$scope.totalScore = _.reduce($scope.scores, function(memo, score) {
			memo.a += score.diffA;
			memo.b += score.diffB;
			memo.d += score.diffDouble;
			memo.x += score.isExchange ? 1 : 0;
			return memo;
		}, {a: 0, b: 0, d: 0, x: 0});
	}
	
	$scope.updateScore();
	
	$scope.timer = {running: false, lastStart: -1, time: 0, displayTime: 0};
    
    $scope.timerValue = function() {
    	var result = $scope.timer.time;
    	if ($scope.timer.running) {
    		result += Date.now() - $scope.timer.lastStart;
    	}
    	return result;
    };
    
    $scope.startTimer = function() {
    	$scope.timer.running = true;
		$scope.timer.lastStart = Date.now();
		$scope.tick();
    }
    $scope.stopTimer = function() {
    	$scope.timer.running = false;
		$scope.timer.displayTime = $scope.timer.time;
    }
    
    $scope.tick = function() {
    	if ($scope.timer.running) {
    		$scope.timer.displayTime = $scope.timerValue();
    		$timeout($scope.tick, 500);
    	}
    };
    
	stateService.change(function(view, state) {
		if (view == "fight") {
			_.extend($scope, state);
			if (state.timer) {
				if (state.timer.action == "start") {
					$scope.startTimer();
				} else {
					$scope.stopTimer();
				}
			}
			$scope.updateScore();
		}
	});
	
	$scope.range = function(n) {
        return new Array(n);
    };
};