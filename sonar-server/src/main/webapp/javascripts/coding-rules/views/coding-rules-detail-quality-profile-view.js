// Generated by CoffeeScript 1.6.3
(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['backbone.marionette', 'common/handlebars-extensions'], function(Marionette) {
    var CodingRulesDetailQualityProfilesView, _ref;
    return CodingRulesDetailQualityProfilesView = (function(_super) {
      __extends(CodingRulesDetailQualityProfilesView, _super);

      function CodingRulesDetailQualityProfilesView() {
        _ref = CodingRulesDetailQualityProfilesView.__super__.constructor.apply(this, arguments);
        return _ref;
      }

      CodingRulesDetailQualityProfilesView.prototype.className = 'coding-rules-detail-quality-profile';

      CodingRulesDetailQualityProfilesView.prototype.template = getTemplate('#coding-rules-detail-quality-profile-template');

      CodingRulesDetailQualityProfilesView.prototype.ui = {
        severitySelect: '.coding-rules-detail-quality-profile-severity'
      };

      CodingRulesDetailQualityProfilesView.prototype.onRender = function() {
        var format;
        format = function(state) {
          if (!state.id) {
            return state.text;
          }
          return "<i class='icon-severity-" + (state.id.toLowerCase()) + "'></i> " + state.text;
        };
        this.ui.severitySelect.val(this.model.get('severity'));
        return this.ui.severitySelect.select2({
          width: '200px',
          minimumResultsForSearch: 999,
          formatResult: format,
          formatSelection: format,
          escapeMarkup: function(m) {
            return m;
          }
        });
      };

      CodingRulesDetailQualityProfilesView.prototype.getParent = function() {
        if (!this.model.get('inherits')) {
          return null;
        }
        return this.options.qualityProfiles.findWhere({
          key: this.model.get('inherits')
        }).toJSON();
      };

      CodingRulesDetailQualityProfilesView.prototype.enhanceParameters = function() {
        var parameters, parent;
        parent = this.getParent();
        parameters = this.model.get('parameters');
        if (!parent) {
          return parameters;
        }
        return parameters.map(function(p) {
          return _.extend(p, {
            original: _.findWhere(parent.parameters, {
              key: p.key
            }).value
          });
        });
      };

      CodingRulesDetailQualityProfilesView.prototype.serializeData = function() {
        return _.extend(CodingRulesDetailQualityProfilesView.__super__.serializeData.apply(this, arguments), {
          parent: this.getParent(),
          parameters: this.enhanceParameters(),
          severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
        });
      };

      return CodingRulesDetailQualityProfilesView;

    })(Marionette.ItemView);
  });

}).call(this);