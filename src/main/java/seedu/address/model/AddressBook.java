package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.parser.ParserUtil.UNSPECIFIED_FIELD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.collections.ObservableList;
import seedu.address.model.person.Person;
import seedu.address.model.person.UniquePersonList;
import seedu.address.model.person.exceptions.DuplicatePersonException;
import seedu.address.model.person.exceptions.NoPlayerException;
import seedu.address.model.person.exceptions.PersonNotFoundException;
import seedu.address.model.tag.Tag;
import seedu.address.model.tag.UniqueTagList;
import seedu.address.model.team.Team;
import seedu.address.model.team.TeamName;
import seedu.address.model.team.UniqueTeamList;
import seedu.address.model.team.exceptions.DuplicateTeamException;
import seedu.address.model.team.exceptions.TeamNotFoundException;

/**
 * Wraps all data at the address-book level
 * Duplicates are not allowed (by .equals comparison)
 */
public class AddressBook implements ReadOnlyAddressBook {

    private final UniquePersonList persons;
    private final UniqueTagList tags;
    private final UniqueTeamList teams;

    /*
     * The 'unusual' code block below is an non-static initialization block, sometimes used to avoid duplication
     * between constructors. See https://docs.oracle.com/javase/tutorial/java/javaOO/initial.html
     *
     * Note that non-static init blocks are not recommended to use. There are other ways to avoid duplication
     *   among constructors.
     */
    {
        persons = new UniquePersonList();
        tags = new UniqueTagList();
        teams = new UniqueTeamList();
    }

    public AddressBook() {}

    /**
     * Creates an AddressBook using the Persons and Tags in the {@code toBeCopied}
     */
    public AddressBook(ReadOnlyAddressBook toBeCopied) {
        this();
        resetData(toBeCopied);
    }

    //// list overwrite operations

    public void setPersons(List<Person> persons) throws DuplicatePersonException {
        this.persons.setPersons(persons);
    }

    public void setTags(Set<Tag> tags) {
        this.tags.setTags(tags);
    }

    public void setTeams(List<Team> teams) throws DuplicateTeamException {
        this.teams.setTeams(teams);
    }

    /**
     * Resets the existing data of this {@code AddressBook} with {@code newData}.
     */
    public void resetData(ReadOnlyAddressBook newData) {
        requireNonNull(newData);
        setTags(new HashSet<>(newData.getTagList()));
        List<Person> syncedPersonList = newData.getPersonList().stream()
                .map(this::syncWithMasterTagList)
                .collect(Collectors.toList());
        List<Team> syncedTeamList = newData.getTeamList();

        try {
            setPersons(syncedPersonList);
            setTeams(syncedTeamList);
        } catch (DuplicatePersonException e) {
            throw new AssertionError("AddressBooks should not have duplicate persons");
        } catch (DuplicateTeamException e) {
            throw new AssertionError("MTM should not have duplicate teams");
        }
    }

    //// person-level operations

    /**
     * Adds a person to the address book.
     * Also checks the new person's tags and updates {@link #tags} with any new tags found,
     * and updates the Tag objects in the person to point to those in {@link #tags}.
     *
     * @throws DuplicatePersonException if an equivalent person already exists.
     */
    public void addPerson(Person p) throws DuplicatePersonException {
        Person person = syncWithMasterTagList(p);
        // TODO: the tags master list will be updated even though the below line fails.
        // This can cause the tags master list to have additional tags that are not tagged to any person
        // in the person list.
        persons.add(person);
    }

    /**
     * Replaces the given person {@code target} in the list with {@code editedPerson}.
     * {@code AddressBook}'s tag list will be updated with the tags of {@code editedPerson}.
     *
     * @throws DuplicatePersonException if updating the person's details causes the person to be equivalent to
     *      another existing person in the list.
     * @throws PersonNotFoundException if {@code target} could not be found in the list.
     *
     * @see #syncWithMasterTagList(Person)
     */
    public void updatePerson(Person target, Person editedPerson)
            throws DuplicatePersonException, PersonNotFoundException {
        requireNonNull(editedPerson);

        Person syncedEditedPerson = syncWithMasterTagList(editedPerson);
        // TODO: the tags master list will be updated even though the below line fails.
        // This can cause the tags master list to have additional tags that are not tagged to any person
        // in the person list.
        if (!editedPerson.getTeamName().toString().equals(UNSPECIFIED_FIELD)) {
            teams.getTeam(editedPerson.getTeamName()).setPerson(target, editedPerson);
        }
        persons.setPerson(target, syncedEditedPerson);
        removeUnusedTags();
    }

    //@@author lohtianwei
    public void sortPlayersBy(String field, String order) throws NoPlayerException {
        persons.sortBy(field, order);
    }

    //@@author
    /**
     *  Updates the master tag list to include tags in {@code person} that are not in the list.
     *  @return a copy of this {@code person} such that every tag in this person points to a Tag object in the master
     *  list.
     */
    private Person syncWithMasterTagList(Person person) {
        final UniqueTagList personTags = new UniqueTagList(person.getTags());
        tags.mergeFrom(personTags);

        // Create map with values = tag object references in the master list
        // used for checking person tag references
        final Map<Tag, Tag> masterTagObjects = new HashMap<>();
        tags.forEach(tag -> masterTagObjects.put(tag, tag));

        // Rebuild the list of person tags to point to the relevant tags in the master tag list.
        final Set<Tag> correctTagReferences = new HashSet<>();
        personTags.forEach(tag -> correctTagReferences.add(masterTagObjects.get(tag)));
        return new Person(
                person.getName(), person.getPhone(), person.getEmail(), person.getAddress(), person.getRemark(),
                person.getTeamName(), correctTagReferences, person.getRating(), person.getPosition(),
                person.getJerseyNumber(), person.getAvatar());
    }

    /**
     * Removes {@code key} from this {@code AddressBook}.
     * @throws PersonNotFoundException if the {@code key} is not in this {@code AddressBook}.
     */
    public boolean removePerson(Person key) throws PersonNotFoundException {
        if (persons.remove(key)) {
            return true;
        } else {
            throw new PersonNotFoundException();
        }
    }

    //// tag-level operations

    public void addTag(Tag t) throws UniqueTagList.DuplicateTagException {
        tags.add(t);
    }

    /**
     *
     * Sets the colour of {@code tag}.
     */
    /** @@author Codee */
    public void setTagColour(Tag tag, String colour) {
        for (Tag t : tags) {
            if (t.getTagName().equals(tag.getTagName())) {
                t.changeTagColour(colour);
            }
        }
    }

    //@@author
    /**
     *
     * Removes {@code tag} from all persons in this {@code AddressBook}.
     */
    public void removeTag(Tag tag) {
        try {
            for (Person person : persons) {
                removeTagFromPerson(tag, person);
            }
        } catch (PersonNotFoundException pnfe) {
            throw new AssertionError("Impossible: AddressBook should contain this person");
        }
    }

    /**
     * Removes {@code tag} from {@code person} in this {@code AddressBook}.
     * @throws PersonNotFoundException if the {@code person} is not in this {@code AddressBook}.
     */
    private void removeTagFromPerson(Tag tag, Person person) throws PersonNotFoundException {
        Set<Tag> newTags = new HashSet<>(person.getTags());

        if (!newTags.remove(tag)) {
            return;
        }

        Person newPerson =
                new Person(person.getName(), person.getPhone(), person.getEmail(), person.getAddress(),
                        person.getRemark(), person.getTeamName(), newTags, person.getRating(), person.getPosition(),
                        person.getJerseyNumber(), person.getAvatar());

        try {
            updatePerson(person, newPerson);
        } catch (DuplicatePersonException dpe) {
            throw new AssertionError("AddressBook should not have duplicate person "
                    + "after updating person's tag.");
        }
    }

    /**
     * Removes all {@code tag} that are not in used by any {@code Person} in this {@code AddressBook}.
     */
    private void removeUnusedTags() {
        Set<Tag> tagsInPersons = persons.asObservableList().stream()
                .map(Person::getTags)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        tags.setTags(tagsInPersons);
    }

    //@@author jordancjq
    /**
     * Creates a team in the manager.
     * @throws DuplicateTeamException if an equivalent team already exists.
     */
    public void createTeam(Team t) throws DuplicateTeamException {
        teams.add(t);
    }

    /**
     * Assigns a {@code person} to a {@code team}.
     * @throws TeamNotFoundException if the {@code team} is not found in this {@code AddressBook}.
     */
    public void assignPersonToTeam(Person person, TeamName teamName) throws DuplicatePersonException {
        teams.assignPersonToTeam(person, teams.getTeam(teamName));

        if (!person.getTeamName().toString().equals(UNSPECIFIED_FIELD)) {
            try {
                removePersonFromTeam(person, person.getTeamName());
            } catch (PersonNotFoundException pnfe) {
                throw new AssertionError("Impossible: Team should contain of this person");
            }
        }

        Person newPersonWithTeam =
                new Person(person.getName(), person.getPhone(), person.getEmail(), person.getAddress(),
                        person.getRemark(), teamName, person.getTags(), person.getRating(), person.getPosition(),
                        person.getJerseyNumber(), person.getAvatar());

        if (!person.getTeamName().equals(newPersonWithTeam.getTeamName())) {
            try {
                updatePerson(person, newPersonWithTeam);
            } catch (DuplicatePersonException dpe) {
                throw new AssertionError("AddressBook should not have duplicate person after assigning team");
            } catch (PersonNotFoundException pnfe) {
                throw new AssertionError("Impossible: AddressBook should contain this person");
            }
        }
    }

    /**
     * Unassigns a {@code person} from team.
     * @throws TeamNotFoundException if the {@code teamName} in {@code person} is {@code UNSPECIFIED_FIELD}.
     */
    public void unassignPersonFromTeam(Person person) throws TeamNotFoundException {
        if (person.getTeamName().toString().equals(UNSPECIFIED_FIELD)) {
            throw new TeamNotFoundException(person.getName().toString());
        }

        Person newPersonWithTeam =
                new Person(person.getName(), person.getPhone(), person.getEmail(), person.getAddress(),
                        person.getRemark(), new TeamName(UNSPECIFIED_FIELD), person.getTags(), person.getRating(),
                        person.getPosition(), person.getJerseyNumber(), person.getAvatar());

        try {
            removePersonFromTeam(person, person.getTeamName());
        } catch (PersonNotFoundException pnfe) {
            throw new AssertionError("Impossible: Team should contain of this person");
        }

        try {
            updatePerson(person, newPersonWithTeam);
        } catch (DuplicatePersonException dpe) {
            throw new AssertionError("AddressBook should not have duplicate person after assigning team");
        } catch (PersonNotFoundException pnfe) {
            throw new AssertionError("Impossible: AddressBook should contain this person");
        }
    }

    /**
     * Immediately add a {@code person} to a {@code team}.
     * @throws TeamNotFoundException if the {@code team} is not found in this {@code AddressBook}.
     */
    public void addPersonToTeam(Person person, TeamName teamName) throws DuplicatePersonException {
        teams.assignPersonToTeam(person, teams.getTeam(teamName));
    }

    /**
     * Removes a {@code person} from a {@code team}.
     */
    private void removePersonFromTeam(Person person, TeamName teamName) throws PersonNotFoundException {
        try {
            teams.removePersonFromTeam(person, teams.getTeam(teamName));
        } catch (PersonNotFoundException pnfe) {
            throw new PersonNotFoundException();
        }
    }

    /**
     * Removes a {@code team} from {@code teams}.
     */
    public void removeTeam(TeamName teamName) throws TeamNotFoundException {
        if (!teams.contains(teamName)) {
            throw new TeamNotFoundException();
        }

        Team teamToRemove = teams.getTeam(teamName);

        for (Person person : teamToRemove) {
            removeTeamFromPerson(person);
        }

        teams.remove(teamToRemove);
    }

    /**
     * Removes {@code teamName} from {@code person} in this {@code Team}.
     */
    private void removeTeamFromPerson(Person person) {
        Person personWithRemoveTeam =
                new Person(person.getName(), person.getPhone(), person.getEmail(), person.getAddress(),
                        person.getRemark(), new TeamName(UNSPECIFIED_FIELD), person.getTags(), person.getRating(),
                        person.getPosition(), person.getJerseyNumber(), person.getAvatar());

        try {
            persons.setPerson(person, personWithRemoveTeam);
        } catch (DuplicatePersonException dpe) {
            throw new AssertionError("AddressBook should not have duplicate person after assigning team");
        } catch (PersonNotFoundException pnfe) {
            throw new AssertionError("Impossible: AddressBook should contain this person");
        }
    }

    /**
     * Renames {@code Team} with {@code updatedTeamName}.
     * @return
     */
    public void renameTeam(Team targetTeam, TeamName updatedTeamName) {
        try {
            List<Person> renameTeamPersonList = new ArrayList<>();

            for (Person person : persons) {
                if (person.getTeamName().equals(targetTeam.getTeamName())) {
                    renameTeamPersonList.add(renameTeamInPerson(person, updatedTeamName, targetTeam));
                }
            }

            Team updatedTeam = new Team(updatedTeamName, renameTeamPersonList);

            teams.setTeam(targetTeam, updatedTeam);
        } catch (DuplicateTeamException dte) {
            throw new AssertionError("AddressBook should not have duplicate team after renaming");
        } catch (TeamNotFoundException tnfe) {
            throw new AssertionError("Impossible: Teams should contain this team");
        }
    }

    /**
     * Renames {@code teamName} in {@code person} with {@code teamName}.
     */
    private Person renameTeamInPerson(Person person, TeamName teamName, Team targetTeam) {
        Person personWithRenameTeam =
                new Person(person.getName(), person.getPhone(), person.getEmail(), person.getAddress(),
                        person.getRemark(), teamName, person.getTags(), person.getRating(),
                        person.getPosition(), person.getJerseyNumber(), person.getAvatar());

        try {
            persons.setPerson(person, personWithRenameTeam);
            return personWithRenameTeam;
        } catch (DuplicatePersonException dpe) {
            throw new AssertionError("AddressBook should not have duplicate person after assigning team");
        } catch (PersonNotFoundException pnfe) {
            throw new AssertionError("Impossible: AddressBook should contain this person");
        }
    }
    //@@author
    //// util methods

    @Override
    public String toString() {
        return persons.asObservableList().size() + " persons, " + tags.asObservableList().size() +  " tags";
        // TODO: refine later
    }

    @Override
    public ObservableList<Person> getPersonList() {
        return persons.asObservableList();
    }

    @Override
    public ObservableList<Tag> getTagList() {
        return tags.asObservableList();
    }

    @Override
    public ObservableList<Team> getTeamList() {
        return teams.asObservableList();
    }

    @Override
    public boolean equals(Object other) {
        return other == this // short circuit if same object
                || (other instanceof AddressBook // instanceof handles nulls
                && this.persons.equals(((AddressBook) other).persons)
                && this.tags.equalsOrderInsensitive(((AddressBook) other).tags));
    }

    @Override
    public int hashCode() {
        // use this method for custom fields hashing instead of implementing your own
        return Objects.hash(persons, tags);
    }
}
