import GenericEntryView from './GenericEntryView.jsx';
import ProjectTypeView from './ProjectTypeView.jsx';
import moment from 'moment';
import UserEntryView from "./UserEntryView.jsx";
import PropTypes from 'prop-types';

class ProjectEntryView extends GenericEntryView {
    static propTypes = {
        entryId: PropTypes.number.isRequired,
        user: PropTypes.string,
        projectTypeId: PropTypes.number,
        title: PropTypes.string,
        created: PropTypes.string
    };

    constructor(props){
        super(props);
        this.endpoint = "/api/project"
    }

    componentWillMount(){
        if(this.props.user && this.props.projectTypeId && this.props.title && this.props.created){
            this.setState({
                content: {
                    user: this.props.user,
                    projectTypeId: this.props.projectTypeId,
                    title: this.props.title,
                    created: this.props.created
                }
            })
        } else {
            super.componentWillMount()
        }
    }

    render(){
        return <span>
            <UserEntryView username={this.state.content.user}/> <ProjectTypeView entryId={this.state.content.projectTypeId}/> project
            &quot;{this.state.content.title}&quot; from {moment(this.state.content.created).format("ddd Do MMM, HH:mm")}

        </span>
    }
}

export default ProjectEntryView;