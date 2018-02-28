import GenericEntryView from './GenericEntryView.jsx';
import ProjectTypeView from './ProjectTypeView.jsx';
import moment from 'moment';
import UserEntryView from "./UserEntryView.jsx";
import PropTypes from 'prop-types';

class ProjectEntryView extends GenericEntryView {
    static propTypes = {
        entryId: PropTypes.number.isRequired
    };

    constructor(props){
        super(props);
        this.endpoint = "/api/project"
    }

    render(){
        return <span>
            <UserEntryView username={this.state.content.user}/> <ProjectTypeView entryId={this.state.content.projectTypeId}/> project
            &quot;{this.state.content.title}&quot; from {moment(this.state.content.created).format("ddd Do MMM, HH:MM")}

        </span>
    }
}

export default ProjectEntryView;