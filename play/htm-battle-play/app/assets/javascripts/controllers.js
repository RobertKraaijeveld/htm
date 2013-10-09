"use strict";

var BattleCtrl = function($scope, $timeout, appService) {
	var _ = window._;
	
	$scope.arena = {name: "Arena 1"};
	$scope.tournament = {name: "Longsword Open"};
	$scope.round = {name: "Round 1"};
	$scope.pool = {order: 3};
    $scope.fights = appService.generateFights(20);
    $scope.currentFight = 1;
    $scope.fightsShowing = [1, 5];
    $scope.fight = _.find($scope.fights, function(f) { return f.index == $scope.currentFight; });
    $scope.possibleScores = [0, 1, 2, 3];
    
    $scope.timer = {'running': false, 'lastStart': -1, 'currentTime': 0};
    
    $scope.toggleTimer = function() {
    	$scope.timer.running = !$scope.timer.running;
    	if ($scope.timer.running) {
    		$timeout($scope.tick, 1000);
    	}
    }
    $scope.tick = function() {
    	if ($scope.timer.running) {
    		$scope.timer.currentTime = $scope.timer.currentTime + 1;
    		$timeout($scope.tick, 1000);
    	}
    };
    
    $scope.fightsBefore = function() {
    	return $scope.fightsShowing[0] > 1;
    };
    
    $scope.fightsAfter = function() {
    	return $scope.fightsShowing[1] < $scope.fights.length;
    };
    
    $scope.beforeRangeFunction = function(item) {
    	return item.index < $scope.fightsShowing[0];
    };
    
    $scope.inRangeFunction = function(item) {
    	return item.index >= $scope.fightsShowing[0] && item.index <= $scope.fightsShowing[1];
    };
    
    $scope.afterRangeFunction = function(item) {
    	return item.index > $scope.fightsShowing[1];
    };
    
    $scope.incCurrentFight = function() {
    	$scope.currentFight = $scope.currentFight + 1;
    	$scope.fightsShowing[0] = Math.max($scope.currentFight - 2, 1);
    	$scope.fightsShowing[1] = Math.min($scope.fightsShowing[0] + 4, $scope.fights.length);
    	$scope.fightsShowing[0] = Math.max($scope.fightsShowing[1] - 4, 1);
    };
    
    $scope.scoreSide = "blue";
    
    $scope.scoreSelected = function(score) {};
    
    $scope.cancelScoreSelect = function () {
    	$('#score-options').hide();
    };
    
    $scope.hitButtonClicked = function(scoreType, side) {
    	$scope.scoreSide = side;
    	if (scoreType == "clean") {
	    	$scope.scoreSelected = function(score) {
	    		$scope.fight.exchanges.push({
	    			time: $scope.timer.currentTime, 
	    			a: side == "red" ? score : 0, 
	    			b: side == "blue" ? score : 0, 
	    			type: scoreType, 
	    			d: 0});
	    		$scope.fight.score[side == "red" ? "a" : "b"] += score;
	    		$('#score-options').hide();
	    	}
    	} else {
    		$scope.scoreSelected = function(score) {
    			var firstScore = score;
    			$scope.scoreSide = side == "red" ? "blue" : "red";
    			$scope.scoreSelected = function(score) {
    				$scope.fight.exchanges.push({
    	    			time: $scope.timer.currentTime, 
    	    			a: side == "red" ? firstScore : score, 
    	    			b: side == "blue" ? firstScore : score, 
    	    			type: scoreType, 
    	    			d: 0});
    	    		$scope.fight.score.a += side == "red" ? firstScore : score;
    	    		$scope.fight.score.b += side == "blue" ? firstScore : score;
    	    		$('#score-options').hide();
    			}
    		}
    	}
    	$('#score-options').show().position({my: "center center", at: "center center", of: "#" + scoreType + "-" + side + "-btn"});
    };
    
    $scope.doubleHitClicked = function() {
    	$scope.fight.exchanges.push({
			time: $scope.timer.currentTime, 
			a: 0, 
			b: 0, 
			type: "double", 
			d: 1});
    	$scope.fight.score.d += 1;
    };
    
    $scope.noHitClicked = function() {
    	$scope.fight.exchanges.push({
			time: $scope.timer.currentTime, 
			a: 0, 
			b: 0, 
			type: "none", 
			d: 0});
    };
    
    $scope.cleanRed = function() {
    	$scope.scoreSide = "red";
    	$scope.scoreSelected = function(score) {
    		$scope.fight.exchanges.push({time: $scope.timer.currentTime, a: score, b: 0, type: "clean", d: 0});
    		$scope.fight.score.a += score;
    		$('#score-options').hide();
    	};
    	$('#score-options').show().position({my: "center center", at: "center center", of: "#clean-red-btn"});
    }
    
    $(document).keypress(function(event) {
		// space
		if (event.keyCode == 32) {
			$scope.toggleTimer();
		}
	});
    
    $('#score-options').hide();
	
};
