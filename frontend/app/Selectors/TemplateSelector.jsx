import React from 'react';
import PropTypes from 'prop-types';

class TemplateSelector extends React.Component {
    static propTypes = {
        selectedTemplate: PropTypes.number.isRequired,
        selectionUpdated: PropTypes.func.isRequired,
        templatesList: PropTypes.array.isRequired
    };

    render(){
        return <select id="project_template_selector" value={this.props.selectedTemplate}
                       onChange={event=>this.props.selectionUpdated(event.target.value)}>
            {
                this.props.templatesList.map(tpl=><option key={tpl.id} value={tpl.id}>{tpl.name}</option>)
            }
        </select>
    }
}

export default TemplateSelector;