angular.module('powerup-admin', ['ui.bootstrap']).
    constant('googleConfig',{
        'apiRoot': document.location.origin + '/_ah/api',
        'client_id': '19047933307-qac31mnnbnf6mk8mtaqobs6stv42n9q3.apps.googleusercontent.com',
        'scope': ['https://www.googleapis.com/auth/plus.login',
        	'https://www.googleapis.com/auth/userinfo.email'],
        immediate: false
    }).
    run(function(googleConfig) { // instance-injector
        var apisToLoad;
        var callback = function() {
            if (--apisToLoad == 0) {
            }
        }
        apisToLoad = 3; // must match number of calls to gapi.client.load()
        gapi.client.load('powerup', 'v1', callback, googleConfig.apiRoot);
        gapi.client.load('oauth2', 'v2', callback);
        gapi.client.load('plus','v1', callback);
    }).
    config(function($routeProvider){
        $routeProvider.
            when('/demo', {templateUrl: 'partials/demo.html', controller: DemoCtrl}).
            when('/event', {templateUrl:'partials/event.html', controller: EventCtrl}).
            otherwise({redirectTo:'/home', templateUrl: 'partials/home.html' });
    }).
    filter('startFrom', function() {
        return function(input, start) {
            start = +start; //parse to int
            return input.slice(start);
        }
    }).
    factory('cerebroServices', ['googleConfig', '$http', function(googleConfig, http) {

        var services = function() {
            return {
                query : function(q, callback) {
                    var request = gapi.client.bigquery.jobs.query({
                        'projectId':'515765323712',
                        'query': q
                    });
                    request.execute(callback);
                },

                signIn: function(mode, callback) {
                    var cfg = {
                        client_id : googleConfig.client_id,
                        mode: mode,
                        scope: googleConfig.scope
                        //, response_type: 'token id_token'
                    }

                    var mainCallback = function(authResult) {
//                        var token = gapi.auth.getToken();
//                        // Use id_token instead of bearer token
//                        token.access_token = token.id_token;
//                        gapi.auth.setToken(token);
                        callback.call(gapi.auth.authorize, authResult)
                    }

                    gapi.auth.authorize(cfg, mainCallback);
                },

                userInfo: function(callback){
                    var request = gapi.client.oauth2.userinfo.get().execute(callback);
                },

                predict:function(csv, callback) {
                    http.post('/churnanalysis', csv).
                        success(callback);
                },

                addEvent: function(evt, callback) {
                    var request = gapi.client.cerebro.event.add(evt);
                    request.execute(callback);
                },

                profile: function(callback) {
                    var request = gapi.client.plus.people.get( {'userId' : 'me'} );
                    request.execute( callback );
                }
            }
        }

        return new services();
    }]).
    directive('json', function() {
        return {
            require: 'ngModel',
            link: function(scope, elm, attrs, ctrl) {
                ctrl.$parsers.unshift(function(viewValue) {
                    try {
                        var obj = angular.fromJson(viewValue)
                        // it is valid
                        ctrl.$setValidity('json', true);
                        return viewValue;
                    } catch(e) {
                        // it is invalid, return undefined (no model update)
                        ctrl.$setValidity('json', false);
                        return undefined;
                    }
                });
            }
        };
    });

function MainCtrl($scope, $location, $log, cerebroServices) {

    var userAuthed = function(authResult) {
        $log.log(authResult)
        if (authResult && !authResult.error) {
            cerebroServices.profile(function(profile) {
                $log.log(profile)
                if (!profile.error) {
                    $scope.signedIn = true;
                    $scope.user = profile;
                    $scope.$apply();
                } else {
                    $log.log('Auth failed!!!')
                }
            });
        }
    };

    cerebroServices.signIn(true, userAuthed)

    $scope.showSignInButton = function() {
        return !$scope.signedIn;
    }

    $scope.setRoute = function(route){
        $location.path(route);
    }

    $scope.signIn = function() {
        if (!$scope.signedIn) {
            cerebroServices.signIn(false, userAuthed)
        } else {
            $scope.signedIn = false
        }
    }

}

function DemoCtrl($scope, $http, $log, cerebroServices) {

    $scope.hcps = []
    $scope.currentPage = 0;
    $scope.pageSize = 10;
    $scope.alerts = [];

    $scope.hcpsFecthed = function (){
        return $scope.hcps.length > 0;
    }

    $scope.closeAlert = function(index) {
        $scope.alerts.splice(index, 1);
    };

    $scope.numberOfPages=function(){
        return Math.ceil($scope.hcps.length/$scope.pageSize);
    }

    $scope.fetchHcps = function() {
        var q = 'SELECT gender, city, state, age, specialty, prescription_all_times, prescription_last_year, prescription_two_years_ago, prescription_last_month, prescription_two_months_ago, prescription_three_months_ago, prescription_four_months_ago, total_visits, sum_ratings_visits, total_calls, total_ratings_calls, avg_time_spent_content, total_content_viewed, guid, name, country FROM [pharmanextbrain.churn_analysis] LIMIT 1000'
        $scope.isFetchingHcps = true;
        cerebroServices.query(q, function(resp) {
            $scope.isFetchingHcps = false;
            $scope.hcps = resp.result.rows
            $scope.$apply()
        });
    }

    $scope.batchAnalysis = function() {
        var startFrom = $scope.currentPage * $scope.pageSize
        for(var i=startFrom; i < (startFrom + $scope.pageSize); i++) {
            var hcp = $scope.hcps[i];
            $scope.performChurnAnalysis(hcp);
        }
    }

    $scope.performChurnAnalysis = function(hcp) {

        if (hcp.churnAnalysisCompleted)
            return

        var csvInstance = []
        for(var i=5; i< hcp.f.length - 3; i++)
            csvInstance.push(hcp.f[i].v)
        hcp.isPredictingChurn = true;
        cerebroServices.predict(csvInstance, function(resp) {
            hcp.isPredictingChurn = false
            hcp.isRisky = (resp.outputLabel === 'churn')
            hcp.churnAnalysisCompleted = true
            //$scope.$apply()
        });
    }

    var assembleHcp = function(raw) {
        return {
            name: raw.f[19].v,
            city: raw.f[1].v,
            state: raw.f[2].v,
            country: raw.f[20].v,
            age: raw.f[3].v,
            specialty: raw.f[4].v
        }
    }

    var assembleContent = function(raw) {
        return {
            name: raw.f[0].v,
            mime: raw.f[1].v,
            url: raw.f[2].v,
            totalView: raw.f[3].v,
            totalPrescriptions: raw.f[4].v
        }
    }

    var groupByProducts = function(rawContentList) {
        var grouped = {};
        angular.forEach(rawContentList, function(raw, i) {
            var content = assembleContent(raw);
            var key = content.name.split(' ').join('_');
            if(this[key]) {
                this[key].push(content);
            } else {
                this[key] = [];
                this[key].push(content);
            }
        }, grouped);
        $log.info(grouped);
        return grouped;
    };

    $scope.recommendContents = function(rawHcp) {
        $scope.isRecommending = true
        var hcp = assembleHcp(rawHcp)
        var replacements = {"%STATE%":hcp.state,"%SPECIALTY%":hcp.specialty}
        var q = "SELECT pc.product.name, pc.mime_type, pc.url, k.total_views, k.total_prescriptions FROM [pharmanextbrain.product_content] as pc INNER JOIN (SELECT t.pguid as guid, t.total as total_views, p.prescription_all_times as total_prescriptions FROM (SELECT guid, prescription_all_times, FROM [pharmanextbrain.churn_analysis] WHERE specialty = '%SPECIALTY%' AND state = '%STATE%') as p INNER JOIN (SELECT data.hcp.guid as hcp_guid, data.content.product.guid as pguid, COUNT(*) as total FROM [pharmanextbrain.events_c] WHERE action = 'content_viewed'AND data.hcp.specialty = '%SPECIALTY%' AND data.hcp.state = '%STATE%'group each by hcp_guid, pguid ) as t ON p.guid = t.hcp_guid ) as k ON k.guid = pc.product.guid order by k.total_views desc,  k.total_prescriptions desc LIMIT 50"
        q = q.replace(/%\w+%/g, function(all) {
            return replacements[all] || all;
        });
        $log.info(q)

        $scope.isFetchingRecommendations = true;
        cerebroServices.query(q, function(resp) {
            $log.info(resp)
            $scope.isFetchingRecommendations = false;
            // Check on status of the Query Job
            if (resp.result.jobComplete) {
                if (resp.result.rows) {
                    $scope.recommendations = groupByProducts(resp.result.rows)

                } else {
                    $scope.recommendation_msg = 'Sorry, no recommendations found for this professional.'
                }
            } else {
                $scope.recommendation_msg = 'Sorry, the request timed out. Try again later.'
            }
            $scope.$apply()
        });
    }

    $scope.requestSalesForceCreation = function(hcp) {
        hcp.isCreatingTask = true

        var description = function(rawHcp) {
            var hcp = assembleHcp(rawHcp)
            var result = ""
            for (var prop in hcp) {
                result += prop + " : " + hcp[prop] + "|";
            }
            return result;
        }

        $http.jsonp('//cit-digital-brain.appspot.com/event/ChurnAlert?callback=JSON_CALLBACK&owner.id=005i0000000aAUsAAM&description='+description(hcp), {'contact.id': '003i00000043jfnAAA'}).
            success(function(data, status){
                hcp.isCreatingTask = false
                hcp.taskCreated = true
                //$scope.alerts.push({msg: "Task successfully created!", type:'success'});
            }).
            error(function(data, status) {
                hcp.isCreatingTask = false
                hcp.taskCreated = false
                $scope.alerts.push({msg: "Error creating task. Try again later.", type:'error'});
            });
    }

    $scope.showProgressBar = function() {
        return $scope.isFetchingHcps
    }

    $scope.showModal = function() {
        return $scope.isRecommending
    }

    $scope.close = function () {
        delete $scope.recommendations
        delete $scope.recommendation_msg
        $scope.isRecommending = false;
    };

}

function EventCtrl($scope, $http, $log, cerebroServices){

    var Evento = function() {
        return {
            action:'',
            data:'',
            asObject:function() {
                return {
                    action: this.action,
                    data: angular.fromJson(this.data)
                }
            }
        }
    };

    $scope.evento = new Evento();
    $scope.savedEvents = [];
    $scope.sampleActions = ["page_viewed","newsletter_viewed","complaint_filed","call"];
    $scope.showSuccessMsg = false;

    $scope.save = function(evento) {
        $log.log($scope.evento);
        cerebroServices.addEvent($scope.evento, function(data) {
            $log.info(data);
            $scope.savedEvents.push( { action: $scope.evento.action, data: $scope.evento.data });
            $scope.showSuccessMsg = true;
            $scope.$apply();
        })
    }

    $scope.fillAction = function(action) {
        $scope.evento.action = action;
    }

    $scope.fillEvent= function(evt) {
        $scope.evento.action = evt.action;
        $scope.evento.data = evt.data;
    }

    $scope.closeSuccessAlert = function() {
        $scope.showSuccessMsg = false;
    }

};