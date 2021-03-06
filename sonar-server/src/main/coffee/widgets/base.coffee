window.SonarWidgets ?= {}

class BaseWidget
  lineHeight: 20


  constructor: ->
    @addField 'components', []
    @addField 'metrics', []
    @addField 'metricsPriority', []
    @addField 'options', []
    @


  addField: (name, defaultValue) ->
    privateName = "_#{name}"
    @[privateName] = defaultValue
    @[name] = (d) -> @param.call @, privateName, d
    @


  param: (name, value) ->
    return @[name] unless value?
    @[name] = value
    @


  addMetric: (property, index) ->
    key = @metricsPriority()[index]
    @[property] = _.extend @metrics()[key],
      key: key
      value: (d) -> d.measures[key]?.val
      formattedValue: (d) -> d.measures[key]?.fval
    @


  trans: (left, top) ->
    "translate(#{left},#{top})"


  render: (container) ->
    @update container
    @


  update: ->
    @


window.SonarWidgets.BaseWidget = BaseWidget