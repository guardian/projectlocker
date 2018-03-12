import GenericEntryView from './GenericEntryView.jsx';
import PropTypes from 'prop-types';
import ProjectTypeView from './ProjectTypeView.jsx';

class ProjectTemplateEntryView extends GenericEntryView {
    static propTypes = {
        entryId: PropTypes.number.isRequired
    };

    constructor(props){
        super(props);
        this.endpoint = "/api/template"
    }

    render(){
        return <span>{this.state.content.name} (<ProjectTypeView entryId={this.state.content.projectTypeId}/>)</span>
    }
}

export default ProjectTemplateEntryView;