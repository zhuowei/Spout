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
package org.spout.engine.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.spout.api.Spout;
import org.spout.api.datatable.GenericDatatableMap;
import org.spout.api.entity.Entity;
import org.spout.api.entity.component.Controller;
import org.spout.api.entity.component.controller.BlockController;
import org.spout.api.entity.component.controller.PlayerController;
import org.spout.api.event.entity.EntityDespawnEvent;
import org.spout.api.event.entity.EntitySpawnEvent;
import org.spout.api.geo.discrete.Point;
import org.spout.api.math.Vector3;
import org.spout.api.player.Player;
import org.spout.api.util.StringMap;

import org.spout.engine.util.thread.snapshotable.SnapshotManager;
import org.spout.engine.util.thread.snapshotable.SnapshotableHashMap;
import org.spout.engine.util.thread.snapshotable.SnapshotableHashSet;
import org.spout.engine.world.SpoutRegion;

/**
 * A class which manages all of the entities within a world.
 */
public class EntityManager implements Iterable<SpoutEntity> {
	/**
	 * The snapshot manager
	 */
	private final SnapshotManager snapshotManager = new SnapshotManager();
	/**
	 * A map of all the entity ids to the corresponding entities.
	 */
	private final SnapshotableHashMap<Integer, SpoutEntity> entities = new SnapshotableHashMap<Integer, SpoutEntity>(snapshotManager);
	/**
	 * A map of entity types to a set containing all entities of that type.
	 */
	private final ConcurrentHashMap<Class<? extends Controller>, SnapshotableHashSet<SpoutEntity>> groupedEntities = new ConcurrentHashMap<Class<? extends Controller>, SnapshotableHashSet<SpoutEntity>>();

	private final SnapshotableHashSet<PlayerController> players = new SnapshotableHashSet<PlayerController>(snapshotManager);
	/**
	 * The next id to check.
	 */
	private final static AtomicInteger nextId = new AtomicInteger(1);
	private final StringMap entityMap = GenericDatatableMap.getStringMap();

	private final Map<Point, Entity> blockEntities = new HashMap<Point, Entity>();

	private SnapshotableHashSet<SpoutEntity> getRawAll(Class<? extends Controller> type) {
		SnapshotableHashSet<SpoutEntity> set = groupedEntities.get(type);
		if (set == null) {
			set = new SnapshotableHashSet<SpoutEntity>(snapshotManager);
			SnapshotableHashSet<SpoutEntity> currentSet = groupedEntities.putIfAbsent(type, set);
			if (currentSet != null) {
				set = currentSet;
			}
		}
		return set;
	}

	/**
	 * Gets all entities with the specified type from the live map.
	 *
	 * @param type The {@link Class} for the type.
	 * @return A set of entities with the specified type.
	 */
	public Set<SpoutEntity> getLiveAll(Class<? extends Controller> type) {
		return getRawAll(type).getLive();
	}

	/**
	 * Gets all entities with the specified type.
	 *
	 * @param type The {@link Class} for the type.
	 * @return A set of entities with the specified type.
	 */
	public Set<SpoutEntity> getAll(Class<? extends Controller> type) {
		return getRawAll(type).get();
	}

	/**
	 * Gets all entities.
	 *
	 * @return A set of entities.
	 */
	public Set<SpoutEntity> getAll() {
		Collection<SpoutEntity> collection = entities.get().values();
		HashSet<SpoutEntity> set = new HashSet<SpoutEntity>(collection.size());
		for (SpoutEntity entity : collection) {
			set.add(entity);
		}
		return set;
	}

	/**
	 * Gets an entity by its id.
	 *
	 * @param id The id.
	 * @return The entity, or {@code null} if it could not be found.
	 */
	public SpoutEntity getEntity(int id) {
		return entities.get().get(id);
	}

	/**
	 * Allocates the id for an entity.
	 *
	 * @param entity The entity.
	 * @return The id.
	 */
	public int allocate(SpoutEntity entity) {
		return allocate(entity, null);
	}

	/**
	 * Allocates the id for an entity.
	 *
	 * @param entity The entity.
	 * @return The id.
	 */
	public int allocate(SpoutEntity entity, SpoutRegion region) {
		EntitySpawnEvent event = new EntitySpawnEvent(entity, entity.getPosition());
		if (event.isCancelled()) {
			return -1; //TODO correct?
		}
		int currentId = entity.getId();
		if (currentId == SpoutEntity.NOTSPAWNEDID) {
			currentId = nextId.getAndIncrement();
			if (currentId == -2) {
				throw new IllegalStateException("No new entity ids left");
			}
			entity.setId(currentId);
		}
		entities.put(currentId, entity);
		entity.setOwningThread(region.getExceutionThread());
		return currentId;
	}

	/**
	 * Deallocates the id for an entity.
	 *
	 * @param entity The entity.
	 */
	public void deallocate(SpoutEntity entity) {
		EntityDespawnEvent event = new EntityDespawnEvent(entity);
		if (event.isCancelled()) {
			return;
		}
		entities.remove(entity.getId());
	}

	public void addEntity(SpoutEntity entity, SpoutRegion region) {
		allocate(entity, region);
		if (entity.getController() != null) {
			getRawAll(entity.getController().getClass()).add(entity);
			Controller c = entity.getController();
			if (c instanceof PlayerController) {
				players.add((PlayerController) c);
			} else if (c instanceof BlockController) {
				Point pos = entity.getPosition();
				Entity old = blockEntities.put(pos, entity);
				if (old != null) {
					old.kill();
				}
			}
		}
	}

	public void removeEntity(SpoutEntity entity) {
		deallocate(entity);
		Controller controller = entity.getController();
		if (controller != null) {
			getRawAll(entity.getController().getClass()).remove(entity);
			if (controller instanceof PlayerController) {
				players.remove((PlayerController)controller);
			} else if (controller instanceof BlockController) {
				Vector3 pos = entity.getPosition().floor();
				Entity be = blockEntities.get(pos);
				if (be == entity) {
					blockEntities.remove(pos);
				}
			}
		}
	}

	/**
	 * Gets all of the player controllers managed this entity manager
	 *
	 * @return players managed by this entity manager
	 */
	public Set<PlayerController> getPlayers() {
		return players.get();
	}

	@Override
	public Iterator<SpoutEntity> iterator() {
		return entities.get().values().iterator();
	}

	public void finalizeRun() {
		// Entity removal and additions happen here
		for (SpoutEntity e : entities.get().values()) {
			e.finalizeRun();
			Controller controller = e.getController();
			if (controller == null) {
				continue;
			}

			controller.finalizeTick();

			if (!(controller instanceof PlayerController)) {
				continue;
			}

			Player p = ((PlayerController) controller).getPlayer();
			if (p.isOnline()) {
				p.getNetworkSynchronizer().finalizeTick();
			}
		}
	}

	public void preSnapshotRun() {
		for (SpoutEntity e : entities.get().values()) {
			Controller controller = e.getController();
			if (controller != null) {
				controller.preSnapshot();
				if (controller instanceof PlayerController) {
					Player p = ((PlayerController) controller).getPlayer();
					if (p.isOnline()) {
						p.getNetworkSynchronizer().preSnapshot();
					}
				}
			}
		}
	}

	/**
	 * Updates the snapshot for all entities
	 */
	public void copyAllSnapshots() {
		for (SpoutEntity e : entities.get().values()) {
			e.copyToSnapshot();
		}
		snapshotManager.copyAllSnapshots();
	}

	/**
	 * Gets the string map associated with this entity manager
	 * @return
	 */
	public StringMap getStringMap() {
		return entityMap;
	}

	/**
	 * The region this entity manager oversees, or null if it does not manage a region's entities
	 *
	 * @return region or null
	 */
	public SpoutRegion getRegion() {
		return null;
	}

	public Map<Point, Entity> getBlockEntities() {
		return Collections.unmodifiableMap(blockEntities);
	}
}
