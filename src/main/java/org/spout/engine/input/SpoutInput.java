/*
 * This file is part of Spout.
 *
 * Copyright (c) 2011-2012, SpoutDev <http://www.spout.org/>
 * Spout is licensed under the SpoutDev License Version 1.
 *
 * Spout is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the SpoutDev License Version 1.
 *
 * Spout is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the SpoutDev License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://www.spout.org/SpoutDevLicenseV1.txt> for the full license,
 * including the MIT license.
 */
package org.spout.engine.input;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spout.api.Spout;
import org.spout.api.keyboard.Input;
import org.spout.engine.SpoutEngine;

import gnu.trove.map.hash.TIntObjectHashMap;

public class SpoutInput implements Input {
	
	TIntObjectHashMap<String> keyCommands = new TIntObjectHashMap<String>();
	TIntObjectHashMap<String> mouseCommands = new TIntObjectHashMap<String>();
	
	
	
	public SpoutInput(){
		bind("KEY_W", "+Forward");
	}
	
	public void doKeypress(int key, boolean pressed){
		String cmd = keyCommands.get(key);
		if(cmd == null) return;
		if(cmd.startsWith("+")){
			if(pressed) Spout.getEngine().processCommand(((SpoutEngine)Spout.getEngine()).getCommandSource(), cmd);
			else {
				cmd = cmd.replaceFirst("+", "-");
				Spout.getEngine().processCommand(((SpoutEngine)Spout.getEngine()).getCommandSource(), cmd);				
			}
		}
		else{
			Spout.getEngine().processCommand(((SpoutEngine)Spout.getEngine()).getCommandSource(), cmd);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see org.spout.engine.input.Input#bind(java.lang.String, java.lang.String)
	 */
	public void bind(String key, String command){		
		if(key.startsWith("KEY")){
			int k = Keyboard.getKeyIndex(key);
			keyCommands.put(k, command);
		}
		if(key.startsWith("MOUSE")){
			int k = Mouse.getButtonIndex(key);
			mouseCommands.put(k, command);
		}
	}
	
	
	public void pollInput(){
		
	}
	
	
}
