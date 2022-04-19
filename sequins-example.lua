engine.name = 'kildare'
s = require 'sequins'

kildare_setup = include 'lib/kildare'
kildare_lfos = include 'lib/kildare_lfos'

-- active_voices = {"bd","sd","tm","cp","rs","cb","hh"}
-- active_voices = {"bd","tm","sd","hh"}
active_voices = {"bd","sd","tm","cp","rs","cb","hh"}
-- active_voices = {'hh'}

function establish()
  bd = s{1,0,0,0,1,0,0,0,1,0,0,1,0,1,0,1}
  sd = s{0,1,0,1,1,0,1,0,0,0,0,0,1,1,1,1}
  tm = s{1,0,1,0,0,0,0,0,1,1,0,0}
  cp = s{0,0,0,0,0,1,0,0,0,0}
  rs = s{0,0,0,1,0,1,0,0,0,0,1}
  cb = s{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1}
  hh = s{1,1,0,0,0,0,1,0,1}
  state = "establish"
end

sync_vals = s{1/4,1/8,1,1/3}

function init()
  kildare_setup.init()
  establish()
  clock.run(parse_patterns)
end

function parse_patterns()
  while true do
    clock.sync(sync_vals())
    for i = 1,#active_voices do
      local iter = _G[active_voices[i]]()
      if iter == 1 then
        engine["trig_"..active_voices[i]]()
      end
    end
    redraw()
  end
end

function reseed()
  for i = 1,#active_voices do
    for j = 1,#_G[active_voices[i]].data do
      _G[active_voices[i]].data[j] = math.random(0,1)
    end
  end
  state = "reseed"
end

function key(n,z)
  if n == 3 and z == 1 then
    reseed()
  elseif n == 2 and z == 1 then
    establish()
  end
end

function redraw()
  screen.clear()
  for i = 1,#active_voices do
    local current_val = _G[active_voices[i]].data[_G[active_voices[i]].ix]
    screen.level(current_val == 1 and 15 or 3)
    screen.move((18*(i-1))+3,32)
    screen.text(active_voices[i])
    screen.move((18*(i-1))+6,42)
    screen.text(current_val)
  end
  screen.move(0,60)
  screen.level(5)
  if state == "establish" then
    screen.text("K3: RESEED")
  else
    screen.text("K2: RE-ESTABLISH / K3: RESEED")
  end
  screen.update()
end

function r()
  norns.script.load(norns.state.script)
end