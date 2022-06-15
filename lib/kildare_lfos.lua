-- adapted from @markeats

local musicutil = require 'musicutil'

local lfos = {}

lfos.NUM_LFOS = 16
lfos.LFO_MIN_TIME = 1 -- Secs
lfos.LFO_MAX_TIME = 60 * 60 * 24
lfos.LFO_UPDATE_FREQ = 128
lfos.LFO_RESOLUTION = 128 -- MIDI CC resolution
lfos.lfo_freqs = {}
lfos.lfo_progress = {}
lfos.lfo_values = {}

local lfo_rates = {1/16,1/8,1/4,5/16,1/3,3/8,1/2,3/4,1,1.5,2,3,4,6,8,16,32,64,128,256,512,1024}
local ivals = {
  ["bd"] = {1,16},
  ["sd"] = {17,32},
  ["tm"] = {33,48},
  ["cp"] = {49,64},
  ["rs"] = {65,80},
  ["cb"] = {81,96},
  ["hh"] = {97,112},
  ["delay"] = {113,128},
  ["reverb"] = {129,144},
  ["main"] = {145,160}
}
local drums = {"bd","sd","tm","cp","rs","cb","hh","delay","reverb","main"}

local delay_params = {'time', 'level', 'feedback', 'spread', 'lpHz', 'hpHz', 'filterQ'}
local reverb_params = {'decay', 'preDelay', 'earlyDiff', 'lpHz', 'modFreq', 'modDepth', 'level', 'thresh', 'slopeBelow', 'slopeAbove'}
local main_params = {'lpHz', 'lpdb', 'lpQ', 'hpHz', 'hpdb', 'hpQ', 'eqHz', 'eqdb', 'eqQ'}

local lfos_loaded = {}

min_specs = {}
max_specs = {}

last_param = {}
for i = 1,lfos.NUM_LFOS do
  last_param[i] = "empty"
end

function lfos.add_params()
  for k,v in pairs(ivals) do
    min_specs[k] = {}
    max_specs[k] = {}
    local i = 1
    if k ~= "delay" and k ~= "reverb" and k ~= "main" then
      for key,val in pairs(kildare_drum_params[k]) do
        if kildare_drum_params[k][key].type ~= "separator" then
          min_specs[k][i] = {min = kildare_drum_params[k][key].min, max = kildare_drum_params[k][key].max, warp = kildare_drum_params[k][key].warp, step = 0.01, default = kildare_drum_params[k][key].default, quantum = 0.01, formatter = kildare_drum_params[k][key].formatter}
          max_specs[k][i] = {min = kildare_drum_params[k][key].min, max = kildare_drum_params[k][key].max, warp = kildare_drum_params[k][key].warp, step = 0.01, default = kildare_drum_params[k][key].max, quantum = 0.01, formatter = kildare_drum_params[k][key].formatter}
          i = i+1 -- do not increment by the separators' gaps...
        end
      end
    else
      for key,val in pairs(kildare_fx_params[k]) do
        if kildare_fx_params[k][key].type ~= "separator" then
          -- if kildare_fx_params[k][key].name ~= "time" then
            min_specs[k][i] = {min = kildare_fx_params[k][key].min, max = kildare_fx_params[k][key].max, warp = kildare_fx_params[k][key].warp, step = 0.01, default = kildare_fx_params[k][key].default, quantum = 0.01, formatter = kildare_fx_params[k][key].formatter}
            max_specs[k][i] = {min = kildare_fx_params[k][key].min, max = kildare_fx_params[k][key].max, warp = kildare_fx_params[k][key].warp, step = 0.01, default = kildare_fx_params[k][key].max, quantum = 0.01, formatter = kildare_fx_params[k][key].formatter}
            i = i+1 -- do not increment by the separators' gaps...
          -- end
        end
      end
    end
  end

  lfos.build_params_static()

  params:add_group("lfos",lfos.NUM_LFOS * 12)
  for i = 1,lfos.NUM_LFOS do
    -- last_param[i] = drums[util.wrap(i,1,7)].."_amp"
    if drums[util.wrap(i,1,#drums)] == "delay" then
      last_param[i] = "time"
    elseif drums[util.wrap(i,1,#drums)] == "reverb" then
      last_param[i] = "decay"
    elseif drums[util.wrap(i,1,#drums)] == "main" then
      last_param[i] = "lpHz"
    else
      last_param[i] = "amp"
    end
    params:add_separator("lfo "..i)
    params:add_option("lfo_"..i,"state",{"off","on"},1)
    params:set_action("lfo_"..i,function(x)
      lfos.sync_lfos(i)
      if x == 1 then
        lfos.return_to_baseline(i,true)
        params:hide("lfo_target_track_"..i)
        params:hide("lfo_target_param_"..i)
        params:hide("lfo_depth_"..i)
        params:hide("lfo_min_"..i)
        params:hide("lfo_max_"..i)
        params:hide("lfo_mode_"..i)
        params:hide("lfo_beats_"..i)
        params:hide("lfo_free_"..i)
        params:hide("lfo_shape_"..i)
        params:hide("lfo_reset_"..i)
        _menu.rebuild_params()
      elseif x == 2 then
        params:show("lfo_target_track_"..i)
        params:show("lfo_target_param_"..i)
        params:show("lfo_depth_"..i)
        params:show("lfo_min_"..i)
        params:show("lfo_max_"..i)
        params:show("lfo_mode_"..i)
        if params:get("lfo_mode_"..i) == 1 then
          params:show("lfo_beats_"..i)
        else
          params:show("lfo_free_"..i)
        end
        params:show("lfo_shape_"..i)
        params:show("lfo_reset_"..i)
        _menu.rebuild_params()
      end
    end)
    params:add_option("lfo_target_track_"..i, "track", drums, util.wrap(i,1,#drums))
    params:set_action("lfo_target_track_"..i,
      function(x)
        local param_id = params.lookup["lfo_target_param_"..i]
        params.params[param_id].options = params_list[drums[x]].names
        params.params[param_id].count = tab.count(params.params[param_id].options)
        -- params:set("lfo_target_param_"..i,1) -- TODO CLEAN THIS UP DURING PSET LAUNCH...
        lfos.rebuild_param("min",i)
        lfos.rebuild_param("max",i)
        lfos.return_to_baseline(i)
      end
    )
    params:add_option("lfo_target_param_"..i, "param",params_list[drums[util.wrap(i,1,#drums)]].names,1)
    params:set_action("lfo_target_param_"..i,
      function(x)
        lfos.rebuild_param("min",i)
        lfos.rebuild_param("max",i)
        lfos.return_to_baseline(i)
      end
    )
    -- params:hide("lfo_"..i)
    params:add_number("lfo_depth_"..i,"depth",0,100,0,function(param) return (param:get().."%") end)
    params:set_action("lfo_depth_"..i, function(x) if x == 0 then lfos.return_to_baseline(i,true) end end)
    params:add{
      type='control',
      id="lfo_min_"..i,
      name="lfo min",
      controlspec = controlspec.new(min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].min,
        min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].max,
        min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].warp,
        min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].step,
        min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].min,
        '',
        min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].quantum)
    }
    params:add{
      type='control',
      id="lfo_max_"..i,
      name="lfo max",
      controlspec = controlspec.new(min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].min,
        min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].max,
        min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].warp,
        min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].step,
        min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].default,
        '',
        min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].quantum)
    }
    params:add_option("lfo_mode_"..i, "update mode", {"clocked bars","free"},1)
    params:set_action("lfo_mode_"..i,
      function(x)
        if x == 1 and params:string("lfo_"..i) == "on" then
          params:hide("lfo_free_"..i)
          params:show("lfo_beats_"..i)
          lfos.lfo_freqs[i] = 1/(lfos.get_the_beats() * lfo_rates[params:get("lfo_beats_"..i)] * 4)
        elseif x == 2 then
          params:hide("lfo_beats_"..i)
          params:show("lfo_free_"..i)
          lfos.lfo_freqs[i] = params:get("lfo_free_"..i)
        end
        _menu.rebuild_params()
      end
      )
    params:add_option("lfo_beats_"..i, "rate", {"1/16","1/8","1/4","5/16","1/3","3/8","1/2","3/4","1","1.5","2","3","4","6","8","16","32","64","128","256","512","1024"},9)
    params:set_action("lfo_beats_"..i,
      function(x)
        if params:string("lfo_mode_"..i) == "clocked bars" then
          lfos.lfo_freqs[i] = 1/(lfos.get_the_beats() * lfo_rates[x] * 4)
        end
      end
    )
    params:add{
      type='control',
      id="lfo_free_"..i,
      name="rate",
      controlspec=controlspec.new(0.001,24,'exp',0.001,0.05,'hz',0.001)
    }
    params:set_action("lfo_free_"..i,
      function(x)
        if params:string("lfo_mode_"..i) == "free" then
          lfos.lfo_freqs[i] = x
        end
      end
    )
    params:add_option("lfo_shape_"..i, "shape", {"sine","square","random"},1)

    params:add_trigger("lfo_reset_"..i, "reset lfo")
    params:set_action("lfo_reset_"..i, function(x) lfos.reset_phase(i) end)

    params:hide("lfo_free_"..i)
  end
  lfos.reset_phase()
  lfos.update_freqs()
  lfos.lfo_update()
  metro.init(lfos.lfo_update, 1 / lfos.LFO_UPDATE_FREQ):start()
  params:set_action("clock_tempo",
  function(bpm)
    local source = params:string("clock_source")
    if source == "internal" then clock.internal.set_tempo(bpm)
    elseif source == "link" then clock.link.set_tempo(bpm) end
    norns.state.clock.tempo = bpm
    if tempo_updater_clock then
      clock.cancel(tempo_updater_clock)
    end
    tempo_updater_clock = clock.run(function() clock.sleep(0.05) lfos.update_tempo() end)
  end)
  -- params:bang()
end

function lfos.update_tempo()
  for i = 1,lfos.NUM_LFOS do
    lfos.sync_lfos(i)
  end
end

function lfos.return_to_baseline(i,silent)
  local drum_target = params:get("lfo_target_track_"..i)
  local param_name = drums[drum_target].."_"..(params_list[drums[drum_target]].ids[(params:get("lfo_target_param_"..i))])
  -- print(drums[drum_target],last_param[i],params:get(drums[drum_target].."_"..last_param[i]))
  if drums[drum_target] ~= "delay" and drums[drum_target] ~= "reverb" and drums[drum_target] ~= "main" then
    if last_param[i] == "time" or last_param[i] == "decay" or last_param[i] == "lpHz" then
      last_param[i] = "poly"
    end
    if last_param[i] ~= "carHz" and last_param[i] ~= "poly" and engine.name == "Kildare" then
      engine.set_param(drums[drum_target],last_param[i],params:get(drums[drum_target].."_"..last_param[i]))
    elseif last_param[i] == "carHz" and engine.name == "Kildare" then
      engine.set_param(drums[drum_target],last_param[i],musicutil.note_num_to_freq(params:get(drums[drum_target].."_"..last_param[i])))
    elseif last_param[i] == "poly" and engine.name == "Kildare" then
      engine.set_param(drums[drum_target],last_param[i],params:get(drums[drum_target].."_"..last_param[i]) == 1 and 0 or 1)
    end
  elseif drums[drum_target] == "delay" and engine.name == "Kildare" then
    -- TODO PREP FOR ANY PARAM...
    if not tab.contains(delay_params,last_param[i]) then
      last_param[i] = delay_params[1]
    end
    -- print(last_param[i],params:get(drums[drum_target].."_"..last_param[i]))
    engine.set_delay_param(last_param[i],params:get(drums[drum_target].."_"..last_param[i]))
  elseif drums[drum_target] == "reverb" and engine.name == "Kildare" then
    -- TODO PREP FOR ANY PARAM...
    if not tab.contains(reverb_params,last_param[i]) then
      last_param[i] = reverb_params[1]
    end
    -- print(last_param[i],params:get(drums[drum_target].."_"..last_param[i]))
    engine.set_reverb_param(last_param[i],params:get(drums[drum_target].."_"..last_param[i]))
  elseif drums[drum_target] == "main" and engine.name == "Kildare" then
    -- TODO PREP FOR ANY PARAM...
    if not tab.contains(main_params,last_param[i]) then
      last_param[i] = main_params[1]
    end
    -- print(last_param[i],params:get(drums[drum_target].."_"..last_param[i]))
    engine.set_main_param(last_param[i],params:get(drums[drum_target].."_"..last_param[i]))
  end
  if not silent then
    last_param[i] = (params_list[drums[drum_target]].ids[(params:get("lfo_target_param_"..i))])
  end
end

function lfos.rebuild_param(param,i) -- TODO: needs to respect number
  local param_id = params.lookup["lfo_"..param.."_"..i]
  local default_value = param == "min" and min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].min
    or params:get(
      params:string("lfo_target_track_"..i)
      .."_"..
      params_list[params:string("lfo_target_track_"..i)].ids[(params:get("lfo_target_param_"..i))])
  if param == "max" then
    if min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].min == default_value then
      default_value = min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].max
    end
  end
  params.params[param_id].controlspec = controlspec.new(min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].min,
    min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].max,
    min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].warp,
    min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].step,
    default_value,
    '',
    min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].quantum)
  if param == "min" then
    if min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].formatter ~= nil then
      params.params[param_id].formatter = min_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].formatter
    end
  end
  if param == "max" then
    if params:string("lfo_target_param_"..i) == "pan" then
      default_value = 1
    end
    params.params[param_id]:set_raw(params.params[param_id].controlspec:unmap(default_value))
    if max_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].formatter ~= nil then
      params.params[param_id].formatter = max_specs[params:string("lfo_target_track_"..i)][params:get("lfo_target_param_"..i)].formatter
    end
  end
end

function lfos.build_params_static()
  params_list = {}
  for i = 1,#drums do
    local style = drums[i]
    params_list[style] = {ids = {}, names = {}}
    if style ~= "delay" and style ~= "reverb" and style ~= "main" then
      for j = 1,#kildare_drum_params[style] do
        if kildare_drum_params[style][j].type ~= "separator" then
          table.insert(params_list[style].ids, kildare_drum_params[style][j].id)
          table.insert(params_list[style].names, kildare_drum_params[style][j].name)
        end
      end
    else
      for j = 1,#kildare_fx_params[style] do
        -- if kildare_fx_params[style][j].type ~= "separator" and kildare_fx_params[style][j].name ~= "time" then
        if kildare_fx_params[style][j].type ~= "separator" then
          table.insert(params_list[style].ids, kildare_fx_params[style][j].id)
          table.insert(params_list[style].names, kildare_fx_params[style][j].name)
        end
      end
    end
  end
end

function lfos.update_freqs()
  for i = 1, lfos.NUM_LFOS do
    lfos.lfo_freqs[i] = 1 / util.linexp(1, lfos.NUM_LFOS, 1, 1, i)
  end
end

function lfos.reset_phase(which)
  if which == nil then
    for i = 1, lfos.NUM_LFOS do
      lfos.lfo_progress[i] = math.pi * 1.5
    end
  else
    lfos.lfo_progress[which] = math.pi * 1.5
  end
end

function lfos.get_the_beats()
  return 60 / params:get("clock_tempo")
end

function lfos.sync_lfos(i)
  if params:get("lfo_mode_"..i) == 1 then
    lfos.lfo_freqs[i] = 1/(lfos.get_the_beats() * lfo_rates[params:get("lfo_beats_"..i)] * 4)
  else
    lfos.lfo_freqs[i] = params:get("lfo_free_"..i)
  end
end

function lfos.lfo_update()
  local delta = (1 / lfos.LFO_UPDATE_FREQ) * 2 * math.pi
  for i = 1,lfos.NUM_LFOS do
    lfos.lfo_progress[i] = lfos.lfo_progress[i] + delta * lfos.lfo_freqs[i]
    local min = params:get("lfo_min_"..i)
    local max = params:get("lfo_max_"..i)
    if min > max then
      local old_min = min
      local old_max = max
      min = old_max
      max = old_min
    end
    local mid = math.abs(min-max)/2
    local percentage = math.abs(max-min) * (params:get("lfo_depth_"..i)/100) -- new
    local target_name = params:string("lfo_target_track_"..i).."_"..params_list[params:string("lfo_target_track_"..i)].ids[(params:get("lfo_target_param_"..i))]
    local value = util.linlin(-1,1,util.clamp(params:get(target_name)-percentage,min,max),util.clamp(params:get(target_name)+percentage,min,max),math.sin(lfos.lfo_progress[i])) -- new
    if value ~= lfos.lfo_values[i] and (params:get("lfo_depth_"..i)/100 > 0) then
      lfos.lfo_values[i] = value
      if params:string("lfo_"..i) == "on" then
        if params:string("lfo_shape_"..i) == "sine" then
          -- engine[target_name](value)
          if params_list[params:string("lfo_target_track_"..i)].ids[(params:get("lfo_target_param_"..i))] == "poly" then
            value = util.linlin(-1,1,min,max,math.sin(lfos.lfo_progress[i])) < mid and 0 or 1
          end
          if engine.name == "Kildare" then
            if params:string("lfo_target_track_"..i) ~= "delay" and params:string("lfo_target_track_"..i) ~= "reverb" and params:string("lfo_target_track_"..i) ~= "main" then
              engine.set_param(params:string("lfo_target_track_"..i),params_list[params:string("lfo_target_track_"..i)].ids[(params:get("lfo_target_param_"..i))],value)
            elseif params:string("lfo_target_track_"..i) == "delay" then
              local delay_param_target = params_list[params:string("lfo_target_track_"..i)].ids[(params:get("lfo_target_param_"..i))]
              if delay_param_target == "time" then
                engine.set_delay_param(delay_param_target,clock.get_beat_sec() * value/128)
              else
                engine.set_delay_param(delay_param_target,value)
              end
              print(delay_param_target,value)
            elseif params:string("lfo_target_track_"..i) == "reverb" then
              local reverb_param_target = params_list[params:string("lfo_target_track_"..i)].ids[(params:get("lfo_target_param_"..i))]
              engine.set_reverb_param(reverb_param_target,value)
            elseif params:string("lfo_target_track_"..i) == "main" then
              local main_param_target = params_list[params:string("lfo_target_track_"..i)].ids[(params:get("lfo_target_param_"..i))]
              engine.set_main_param(main_param_target,value)
            end
          end
        elseif params:string("lfo_shape_"..i) == "square" then
          -- engine[target_name](value >= mid and max or min)
          if engine.name == "Kildare" then
            if params:string("lfo_target_track_"..i) ~= "delay" then
              engine.set_param(params:string("lfo_target_track_"..i),params_list[params:string("lfo_target_track_"..i)].ids[(params:get("lfo_target_param_"..i))],value >= mid and max or min)
            elseif params:string("lfo_target_track_"..i) == "delay" then
              local delay_param_target = params_list[params:string("lfo_target_track_"..i)].ids[(params:get("lfo_target_param_"..i))]
              engine.set_delay_param(delay_param_target,value)
            end
          end
        elseif params:string("lfo_shape_"..i) == "random" then
          local rand_calc = util.linlin(-1,1,min,max,math.sin(lfos.lfo_progress[i]))
          print(rand_calc) -- TODO DIES OFF...it'll just never come close when it's going at a high rate...
          if util.round(rand_calc,0.001) == min or util.round(rand_calc,0.001) == max then
            if min < max then
              local send_value = math.random(util.round(util.clamp(params:get(target_name)-percentage,min,max)*100),util.round(util.clamp(params:get(target_name)+percentage,min,max)*100))/100
              -- engine[target_name](send_value)
              print(params:string("lfo_target_track_"..i),params_list[params:string("lfo_target_track_"..i)].ids[(params:get("lfo_target_param_"..i))],send_value)
              if engine.name == "Kildare" then
                engine.set_param(params:string("lfo_target_track_"..i),params_list[params:string("lfo_target_track_"..i)].ids[(params:get("lfo_target_param_"..i))],send_value)
              end
            else
              local send_value = math.random(util.round(util.clamp(params:get(target_name)+percentage,min,max)*100),util.round(util.clamp(params:get(target_name)-percentage,min,max)*100))/100
              -- engine[target_name](send_value)
              if engine.name == "Kildare" then
                engine.set_param(params:string("lfo_target_track_"..i),params_list[params:string("lfo_target_track_"..i)].ids[(params:get("lfo_target_param_"..i))],send_value)
              end
            end
          end
        end
      end
    end
  end
end

return lfos