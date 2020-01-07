package org.mattrr78.sakilaql;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActorService {

    private final ActorJpaRepo repo;

    ActorService(ActorJpaRepo repo)  {
        this.repo = repo;
    }

    public List<Actor> findAll()  {
        return repo.findAll();
    }

}
