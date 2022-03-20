local Kildare = {}
local specs = {}
local ControlSpec = require "controlspec"

specs.amp = ControlSpec.new(0, 5, "lin", 0, 1)
specs.carHz = ControlSpec.new(20, 24000, "lin", 0, 55, "hz")
specs.carAtk = ControlSpec.new(0.001, 10, "lin", 0, 0, "s")
specs.carRel = ControlSpec.new(0.001, 10, "lin", 0, 0.3, "s")
specs.modHz = ControlSpec.new(20, 24000, "lin", 0, 600, "hz")
specs.modAmp = ControlSpec.new(0, 127, "lin", 0, 127)
specs.modAtk = ControlSpec.new(0.001, 10, "lin", 0, 0, "s")
specs.modRel = ControlSpec.new(0.001, 10, "lin", 0, 0.3, "s")
specs.feedAmp = ControlSpec.new(0, 10, "lin", 0, 10)
specs.pan = ControlSpec.new(-1, 1, "lin", 0, 0)
specs.rampDepth = ControlSpec.new(0, 2, "lin", 0, 0.5)
specs.rampDec = ControlSpec.new(0.001, 10, "lin", 0, 0.3, "s")
specs.SPTCH = ControlSpec.new(1, 10, "lin", 1, 1)
specs.SCHNK = ControlSpec.new(1, 10, "lin", 1, 1)
specs.AMD = ControlSpec.new(0, 2, "lin", 0, 1)
specs.AMF = ControlSpec.new(20, 24000, "lin", 0, 2698.8, "hz")
specs.EQF = ControlSpec.new(20, 24000, "lin", 0, 6000, "hz")
specs.EQG = ControlSpec.new(0, 10, "lin", 0, 0)
specs.brate = ControlSpec.new(20, 24000, "lin", 0, 24000, "hz")
specs.bcnt = ControlSpec.new(1, 24, "lin", 0, 24)
specs.click = ControlSpec.new(0, 1, "lin", 0, 0)
specs.LPfreq = ControlSpec.new(20, 24000, "exp", 0, 19000, "hz")
specs.HPfreq = ControlSpec.new(20, 24000, "exp", 0, 0, "hz")
specs.filterQ = ControlSpec.new(0.001, 2, "lin", 0, 1)

function Kildare.init()
  
  bd_params = {
    {'amp',1},
    {'carHz',55},
    {'carAtk',0},
    {'carRel',0.3},
    {'modHz',600},
    {'modAmp',127},
    {'modAtk',0},
    {'modRel',0.05},
    {'feedAmp',10},
    {'pan',0},
    {'rampDepth',0.5},
    {'rampDec',0.3},
    {'SPTCH',1},
    {'SCHNK',1},
    {'AMD',1},
    {'AMF',2698.8},
    {'EQF',6000},
    {'EQG',0},
    {'brate',24000},
    {'bcnt',24},
    {'click',1},
    {'LPfreq',19000},
    {'HPfreq',0},
    {'filterQ',1},
  }
  
  for i = 1,#bd_params do
    params:add_control("bd_"..bd_params[i][1], bd_params[i][1], specs[bd_params[i][1]])
    params:set_action("bd_"..bd_params[i][1], function(x)
      engine["bd_"..bd_params[i][1]](x)
    end)
  end
  
  params:bang()
  
end

return Kildare