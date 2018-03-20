import React from 'react';
import PropTypes from 'prop-types';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';
import TemplateSelector from "../../Selectors/TemplateSelector.jsx";

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
                    <td><TemplateSelector selectedTemplate={this.props.selectedTemplate}
                                          selectionUpdated={this.props.selectionUpdated}
                                          templatesList={this.props.templatesList}/></td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default TemplateComponent;
