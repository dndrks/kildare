-- adapted from @markeats

local musicutil = require 'musicutil'

local lfos = {}
lfos.count = 16
lfos.update_freq = 128
lfos.freqs = {}
lfos.progress = {}
lfos.values = {}
lfos.rand_values = {}

lfos.rates = {1/16,1/8,1/4,5/16,1/3,3/8,1/2,3/4,1,1.5,2,3,4,6,8,16,32,64,128,256,512,1024}
lfos.rates_as_strings = {"1/16","1/8","1/4","5/16","1/3","3/8","1/2","3/4","1","1.5","2","3","4","6","8","16","32","64","128","256","512","1024"}

lfos.targets = {}

lfos.specs = {}

lfos.params_list = {}
lfos.last_param = {}
for i = 1,lfos.count do
  lfos.last_param[i] = "empty"
end

local ivals = {}

function lfos.add_params(drum_names, fx_names, poly)

  for k,v in pairs(drum_names) do
    table.insert(lfos.targets,v)
  end

  for k,v in pairs(fx_names) do
    table.insert(lfos.targets,v)
  end

  for k,v in pairs(lfos.targets) do
    ivals[v] = {1 + (16*(k-1)), (16 * k)}
  end

  for fx,parameters in pairs(kildare_fx_params) do
    lfos[fx.."_params"] = {}
    for inner,prm in pairs(kildare_fx_params[fx]) do
      if prm.type ~= "separator" then
        table.insert(lfos[fx.."_params"], prm.id)
      end
    end
  end

  for k,v in pairs(ivals) do
    lfos.specs[k] = {}
    local i = 1

    -- t values:
    -- 0: separator
    -- 1: number
    -- 2: option
    -- 3: control
    -- 4: file
    -- 5: taper
    -- 6: trigger
    -- 7: group
    -- 8: text
    -- 9: binary

    local param_group = (k ~= "delay" and k ~= "reverb" and k ~= "main") and kildare_drum_params or kildare_fx_params
    for key,val in pairs(param_group[k]) do
      if param_group[k][key].type ~= "separator" then
        if (poly == nil and val.id ~= "poly") or (poly == true) then
          local system_id = params.lookup[k.."_"..param_group[k][key].id]
          local quantum_size;
          if params.params[system_id].controlspec ~= nil then
            quantum_size = params.params[system_id].controlspec.quantum
          else
            quantum_size = param_group[k][key].quantum ~= nil and param_group[k][key].quantum or 0.01
          end
          lfos.specs[k][i] = {
            min = param_group[k][key].min,
            max = param_group[k][key].max,
            warp = param_group[k][key].warp,
            step = 0,
            default = param_group[k][key].default,
            quantum = quantum_size,
            formatter = param_group[k][key].formatter
          }
          i = i+1 -- do not increment by the separators' gaps...
        end
      end
    end
  end

  lfos.build_params_static(poly)

  params:add_group("lfos",lfos.count * 12)
  for i = 1,lfos.count do
    if lfos.targets[util.wrap(i,1,#lfos.targets)] == "delay" then
      lfos.last_param[i] = "time"
    elseif lfos.targets[util.wrap(i,1,#lfos.targets)] == "reverb" then
      lfos.last_param[i] = "decay"
    elseif lfos.targets[util.wrap(i,1,#lfos.targets)] == "main" then
      lfos.last_param[i] = "lSHz"
    else
      if poly then
        lfos.last_param[i] = "poly"
      else
        lfos.last_param[i] = "amp"
      end
    end
    params:add_separator("lfo "..i)
    params:add_option("lfo_"..i,"state",{"off","on"},1)
    params:set_action("lfo_"..i,function(x)
      lfos.sync_lfos(i)
      if x == 1 then
        lfos.return_to_baseline(i,true,poly)
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
    params:add_option("lfo_target_track_"..i, "track", lfos.targets, 1)
    params:set_action("lfo_target_track_"..i,
      function(x)
        local param_id = params.lookup["lfo_target_param_"..i]
        params.params[param_id].options = lfos.params_list[lfos.targets[x]].names
        params.params[param_id].count = tab.count(params.params[param_id].options)
        lfos.rebuild_param("min",i)
        lfos.rebuild_param("max",i)
        lfos.return_to_baseline(i,nil,poly)
        params:set("lfo_target_param_"..i,1)
        params:set("lfo_depth_"..i,0)
        lfos.reset_bounds_in_menu(i)

      end
    )
    params:add_option("lfo_target_param_"..i, "param",lfos.params_list[lfos.targets[1]].names,1)
    params:set_action("lfo_target_param_"..i,
      function(x)
        lfos.rebuild_param("min",i)
        lfos.rebuild_param("max",i)
        lfos.return_to_baseline(i,nil,poly)
        lfos.reset_bounds_in_menu(i)
      end
    )
    params:add_number("lfo_depth_"..i,"depth",0,100,0,function(param) return (param:get().."%") end)
    params:set_action("lfo_depth_"..i, function(x) if x == 0 then lfos.return_to_baseline(i,true,poly) end end)

    local target_track = params:string("lfo_target_track_"..i)
    local target_param = params:get("lfo_target_param_"..i)
    params:add{
      type='control',
      id="lfo_min_"..i,
      name="lfo min",
      controlspec = controlspec.new(
        lfos.specs[target_track][target_param].min,
        lfos.specs[target_track][target_param].max,
        lfos.specs[target_track][target_param].warp,
        lfos.specs[target_track][target_param].step,
        lfos.specs[target_track][target_param].min,
        '',
        lfos.specs[target_track][target_param].quantum
      )
    }
    params:add{
      type='control',
      id="lfo_max_"..i,
      name="lfo max",
      controlspec = controlspec.new(
        lfos.specs[target_track][target_param].min,
        lfos.specs[target_track][target_param].max,
        lfos.specs[target_track][target_param].warp,
        lfos.specs[target_track][target_param].step,
        lfos.specs[target_track][target_param].default,
        '',
        lfos.specs[target_track][target_param].quantum
      )
    }
    params:add_option("lfo_mode_"..i, "update mode", {"clocked bars","free"},1)
    params:set_action("lfo_mode_"..i,
      function(x)
        if x == 1 and params:string("lfo_"..i) == "on" then
          params:hide("lfo_free_"..i)
          params:show("lfo_beats_"..i)
          lfos.freqs[i] = 1/(lfos.get_the_beats() * lfos.rates[params:get("lfo_beats_"..i)] * 4)
        elseif x == 2 then
          params:hide("lfo_beats_"..i)
          params:show("lfo_free_"..i)
          lfos.freqs[i] = params:get("lfo_free_"..i)
        end
        _menu.rebuild_params()
      end
      )
    params:add_option("lfo_beats_"..i, "rate", lfos.rates_as_strings, tab.key(lfos.rates_as_strings,"1"))
    params:set_action("lfo_beats_"..i,
      function(x)
        if params:string("lfo_mode_"..i) == "clocked bars" then
          lfos.freqs[i] = 1/(lfos.get_the_beats() * lfos.rates[x] * 4)
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
          lfos.freqs[i] = x
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
  metro.init(lfos.lfo_update, 1 / lfos.update_freq):start()
  
  local system_tempo_change_handler = params:lookup_param("clock_tempo").action
  
  local new_tempo_change_handler = function(bpm)
    system_tempo_change_handler(bpm)
    if lfos.tempo_updater_clock then
      clock.cancel(lfos.tempo_updater_clock)
    end
    lfos.tempo_updater_clock = clock.run(function() clock.sleep(0.05) lfos.update_tempo() end)
  end

  params:set_action("clock_tempo", new_tempo_change_handler)

end

function lfos.update_tempo()
  for i = 1,lfos.count do
    lfos.sync_lfos(i)
  end
end

function lfos.reset_bounds_in_menu(i)
  local target_track = params:string("lfo_target_track_"..i)
  local target_param = params:get("lfo_target_param_"..i)
  local restore_min = lfos.specs[target_track][target_param].min
  local restore_max = params:get(target_track.."_"..lfos.params_list[target_track].ids[(target_param)])
  if restore_min == restore_max then
    restore_max = lfos.specs[target_track][target_param].max
  end
  if params:string("lfo_target_param_"..i) == "pan" then
    restore_max = 1
  end
  params:set("lfo_min_"..i, restore_min)
  params:set("lfo_max_"..i, restore_max)
end

function lfos.return_to_baseline(i,silent,poly)
  local drum_target = params:get("lfo_target_track_"..i)
  local parent = lfos.targets[drum_target]
  local param_name = parent.."_"..(lfos.params_list[parent].ids[(params:get("lfo_target_param_"..i))])
  if parent ~= "delay" and parent ~= "reverb" and parent ~= "main" then
    if lfos.last_param[i] == "time" or lfos.last_param[i] == "decay" or lfos.last_param[i] == "lSHz" then
      if poly then
        lfos.last_param[i] = "poly"
      else
        lfos.last_param[i] = "amp"
      end
    end
    if lfos.last_param[i] ~= "carHz" and lfos.last_param[i] ~= "poly" and engine.name == "Kildare" then
      engine.set_voice_param(parent,lfos.last_param[i],params:get(parent.."_"..lfos.last_param[i]))
    elseif lfos.last_param[i] == "carHz" and engine.name == "Kildare" then
      engine.set_voice_param(parent,lfos.last_param[i],musicutil.note_num_to_freq(params:get(parent.."_"..lfos.last_param[i])))
    elseif lfos.last_param[i] == "poly" and engine.name == "Kildare" then
      engine.set_voice_param(parent,lfos.last_param[i],params:get(parent.."_"..lfos.last_param[i]) == 1 and 0 or 1)
    end
  elseif (parent == "delay" or parent == "reverb" or parent == "main") and engine.name == "Kildare" then
    local sources = {delay = lfos.delay_params, reverb = lfos.reverb_params, main = lfos.main_params}
    if not tab.contains(sources[parent],lfos.last_param[i]) then
      lfos.last_param[i] = sources[parent][1]
    end
    if parent == "delay" and lfos.last_param[i] == "time" then
      engine["set_"..parent.."_param"](lfos.last_param[i],clock.get_beat_sec() * params:get(parent.."_"..lfos.last_param[i])/128)
    else
      engine["set_"..parent.."_param"](lfos.last_param[i],params:get(parent.."_"..lfos.last_param[i]))
    end
  end
  if not silent then
    lfos.last_param[i] = (lfos.params_list[parent].ids[(params:get("lfo_target_param_"..i))])
  end
end

function lfos.rebuild_param(param,i)
  local param_id = params.lookup["lfo_"..param.."_"..i]
  local target_track = params:string("lfo_target_track_"..i)
  local target_param = params:get("lfo_target_param_"..i)
  local default_value = param == "min" and lfos.specs[target_track][target_param].min
    or params:get(target_track.."_"..lfos.params_list[target_track].ids[(target_param)])
  if param == "max" then
    if lfos.specs[target_track][target_param].min == default_value then
      default_value = lfos.specs[target_track][target_param].max
    end
  end
  params.params[param_id].controlspec = controlspec.new(
    lfos.specs[target_track][target_param].min,
    lfos.specs[target_track][target_param].max,
    lfos.specs[target_track][target_param].warp,
    lfos.specs[target_track][target_param].step,
    default_value,
    '',
    lfos.specs[target_track][target_param].quantum
  )
  if param == "max" then
    if params:string("lfo_target_param_"..i) == "pan" then
      default_value = 1
    end
    params.params[param_id]:set_raw(params.params[param_id].controlspec:unmap(default_value))
  end
  if lfos.specs[target_track][target_param].formatter ~= nil then
    params.params[param_id].formatter = lfos.specs[target_track][target_param].formatter
  end
end

function lfos.build_params_static(poly)
  for i = 1,#lfos.targets do
    local style = lfos.targets[i]
    lfos.params_list[style] = {ids = {}, names = {}}
    local parent = (style ~= "delay" and style ~= "reverb" and style ~= "main") and kildare_drum_params[style] or kildare_fx_params[style]
    for j = 1,#parent do
      if parent[j].type ~= "separator" then
        if (parent[j].id == "poly" and poly) or (parent[j].id ~= "poly") then
          table.insert(lfos.params_list[style].ids, parent[j].id)
          table.insert(lfos.params_list[style].names, parent[j].name)
        end
      end
    end

  end
end

function lfos.update_freqs()
  for i = 1, lfos.count do
    lfos.freqs[i] = 1 / util.linexp(1, lfos.count, 1, 1, i)
  end
end

function lfos.reset_phase(which)
  if which == nil then
    for i = 1, lfos.count do
      lfos.progress[i] = math.pi * 1.5
    end
  else
    lfos.progress[which] = math.pi * 1.5
  end
end

function lfos.get_the_beats()
  return 60 / params:get("clock_tempo")
end

function lfos.sync_lfos(i)
  if params:get("lfo_mode_"..i) == 1 then
    lfos.freqs[i] = 1/(lfos.get_the_beats() * lfos.rates[params:get("lfo_beats_"..i)] * 4)
  else
    lfos.freqs[i] = params:get("lfo_free_"..i)
  end
end

function lfos.set_delay_param(param_target,value)
  if param_target == "time" then
    engine.set_delay_param(param_target,clock.get_beat_sec() * value/128)
  else
    engine.set_delay_param(param_target,value)
  end
end

function lfos.send_param_value(target_track, target_id, value)
  if target_track ~= "delay" and target_track ~= "reverb" and target_track ~= "main" then
    if target_id == "carHz" then
      value = musicutil.note_num_to_freq(value)
    end
    engine.set_voice_param(target_track,target_id,value)
  else
    if target_track == "delay" then
      lfos.set_delay_param(target_id,value)
    else
      engine["set_"..target_track.."_param"](target_id,value)
    end
  end
end

function lfos.lfo_update()
  local delta = (1 / lfos.update_freq) * 2 * math.pi
  for i = 1,lfos.count do
    lfos.progress[i] = lfos.progress[i] + delta * lfos.freqs[i]
    local min = params:get("lfo_min_"..i)
    local max = params:get("lfo_max_"..i)
    if min > max then
      local old_min = min
      local old_max = max
      min = old_max
      max = old_min
    end

    local mid = math.abs(min-max)/2 + min
    local percentage = math.abs(min-max) * (params:get("lfo_depth_"..i)/100)
    local target_track = params:string("lfo_target_track_"..i)
    local target_param = params:get("lfo_target_param_"..i)
    local param_name = lfos.params_list[target_track]
    local engine_target = params:get(target_track.."_"..param_name.ids[(target_param)])
    scaled_min = min
    scaled_max = min + percentage
    local value = util.linlin(-1,1,scaled_min,scaled_max,math.sin(lfos.progress[i])) -- new
    mid = util.linlin(min,max,scaled_min,scaled_max,mid) -- new
    
    if value ~= lfos.values[i] and (params:get("lfo_depth_"..i)/100 > 0) then
      lfos.values[i] = value
      if params:string("lfo_"..i) == "on" then
        if params:string("lfo_shape_"..i) == "sine" then
          if param_name.ids[(target_param)] == "poly" then
            value = util.linlin(-1,1,min,max,math.sin(lfos.progress[i])) < mid and 0 or 1
          end
          lfos.send_param_value(target_track, param_name.ids[(target_param)], value)
        elseif params:string("lfo_shape_"..i) == "square" then
          local square_value = value >= mid and max or min
          square_value = util.linlin(min,max,scaled_min,scaled_max,square_value) -- new
          lfos.send_param_value(target_track, param_name.ids[(target_param)], square_value)
        elseif params:string("lfo_shape_"..i) == "random" then
          local prev_value = lfos.rand_values[i]
          lfos.rand_values[i] = value >= mid and max or min
          local rand_value;
          if prev_value ~= lfos.rand_values[i] then
            rand_value = util.linlin(min,max,scaled_min,scaled_max,math.random(math.floor(min*100),math.floor(max*100))/100) -- new
            lfos.send_param_value(target_track, param_name.ids[(target_param)], rand_value)
          end
        end
      end
    end
  end
end

return lfos