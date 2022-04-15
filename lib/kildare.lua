local Kildare = {}
local specs = {}
local ControlSpec = require "controlspec"

local types = {"bd","sd"}

function Kildare.init()
  
  drum_params = {
    ["bd"] = {
      {id = 'amp', name = 'amp', type = 'control', min = 0, max = 5, warp = 'lin', default = 1.9},
      {id = 'carHz', name = 'carrier hz', type = 'control', min = 20, max = 1000, warp = 'exp', default = 55},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.3},
      {id = 'modHz', name = 'modulator hz', type = 'control', min = 20, max = 24000, warp = 'lin', default = 600},
      {id = 'modAmp', name = 'modulator amp', type = 'number', min = 0, max = 127, default = 0},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05},
      {id = 'feedAmp', name = 'feedback amp', type = 'control', min = 0, max = 10, warp = 'lin', default = 10},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0},
      {id = 'rampDepth', name = 'ramp depth', type = 'control', min = 0, max = 2, warp = 'lin', default = 0.5},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.3},
      {id = 'SPTCH', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1},
      {id = 'SCHNK', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1},
      {id = 'AMD', name = 'amp mod depth', type = 'control', min = 0, max = 2, warp = 'lin', default = 0},
      {id = 'AMF', name = 'amp mod frequency', type = 'control', min = 20, max = 12000, warp = 'exp', default = 8175.08},
      {id = 'EQF', name = 'eq frequency', type = 'control', min = 20, max = 24000, warp = 'exp', default = 6000},
      {id = 'EQG', name = 'eq gain', type = 'control', min = 0, max = 10, warp = 'lin', default = 0},
      {id = 'brate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000},
      {id = 'bcnt', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'LPfreq', name = 'lo-pass frequency', type = 'control', min = 20, max = 24000, warp = 'exp', default = 19000},
      {id = 'HPfreq', name = 'hi-pass frequency', type = 'control', min = 20, max = 24000, warp = 'exp', default = 0},
      {id = 'filterQ', name = 'filter q', type = 'control', min = 0.001, max = 2, warp = 'lin', default = 1}
    },
    ["sd"] = {
      {id = 'amp', name = 'amp', type = 'control', min = 0, max = 5, warp = 'lin', default = 1},
      {id = 'carHz', name = 'carrier hz', type = 'control', min = 123, max = 660, warp = 'exp', default = 293.7},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.3},
      {id = 'modHz', name = 'modulator hz', type = 'control', min = 20, max = 24000, warp = 'lin', default = 2770},
      {id = 'modAmp', name = 'modulator amp', type = 'number', min = 0, max = 127, default = 0},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.2},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 1},
      {id = 'noiseAmp', name = 'noise amp', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.01},
      {id = 'noiseAtk', name = 'noise attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0},
      {id = 'noiseRel', name = 'noise release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.1},
      {id = 'feedAmp', name = 'feedback amp', type = 'control', min = 0, max = 10, warp = 'lin', default = 0},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0},
      {id = 'rampDepth', name = 'ramp depth', type = 'control', min = 0, max = 2, warp = 'lin', default = 1},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.06},
      {id = 'SPTCH', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1},
      {id = 'SCHNK', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1},
      {id = 'AMD', name = 'amp mod depth', type = 'control', min = 0, max = 2, warp = 'lin', default = 0},
      {id = 'AMF', name = 'amp mod frequency', type = 'control', min = 20, max = 12000, warp = 'exp', default = 2698.8},
      {id = 'EQF', name = 'eq frequency', type = 'control', min = 20, max = 24000, warp = 'exp', default = 12000},
      {id = 'EQG', name = 'eq gain', type = 'control', min = 0, max = 10, warp = 'lin', default = 10},
      {id = 'brate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000},
      {id = 'bcnt', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'LPfreq', name = 'lo-pass frequency', type = 'control', min = 20, max = 24000, warp = 'exp', default = 19000},
      {id = 'HPfreq', name = 'hi-pass frequency', type = 'control', min = 20, max = 24000, warp = 'exp', default = 0},
      {id = 'filterQ', name = 'filter q', type = 'control', min = 0.001, max = 2, warp = 'lin', default = 1}
    }
  }

  params:add_separator("kildare")
  
  for k,v in pairs(drum_params) do
    params:add_group(k,tab.count(drum_params[k]))
    for i = 1,tab.count(drum_params[k]) do
      local d = drum_params[k][i]
      if d.type == 'control' then
        params:add{
          type = 'control',
          id = k.."_"..d.id,
          name = d.name,
          controlspec = ControlSpec.new(d.min, d.max, d.warp, 0, d.default)
        }
      elseif d.type == 'number' then
        params:add{
          type = 'number',
          id = k.."_"..d.id,
          name = d.name,
          min = d.min,
          max = d.max,
          default = d.default
        }
      end
      params:set_action(k.."_"..d.id, function(x)
        engine[k.."_"..d.id](x)
      end)
    end
  end
  
  params:bang()
  
end

return Kildare