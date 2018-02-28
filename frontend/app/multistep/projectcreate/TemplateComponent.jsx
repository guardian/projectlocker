import React from 'react';
import PropTypes from 'prop-types';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';

class TemplateComponent extends CommonMultistepComponent {
    static propTypes = {
        templatesList: PropTypes.array.isRequired,
        selectedTemplate: PropTypes.number.isRequired,
        selectionUpdated: PropTypes.func.isRequired
    };

    constructor(props){
        super(props);
    }

    render(){
        return <div>
            <h3>Select project template</h3>
            <p>The first piece of information we need is a template to base your new project on.</p>
            <table>
                <tbody>
                <tr>
                    <td>Project Template</td>
                    <td><select id="project_template_selector" value={this.props.selectedTemplate}
                                onChange={event=>this.props.selectionUpdated(event.target.value)}>
                        {
                            this.props.templatesList.map(tpl=><option key={tpl.id} value={tpl.id}>{tpl.name}</option>)
                        }
                    </select></td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default TemplateComponent;
