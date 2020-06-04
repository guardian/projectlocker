import React from 'react';
import PropTypes from 'prop-types';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';

class CommissionTitleComponent extends CommonMultistepComponent {
    static propTypes = {
        projectName: PropTypes.string.isRequired,
        selectionUpdated: PropTypes.func.isRequired
    };

    render(){
        return <div>
            <h3>Name your commission</h3>
            <p>Now, we need a descriptive name for your new commission</p>
            <table>
                <tbody>
                <tr>
                    <td>Commission Name</td>
                    <td><input id="projectNameInput" onChange={(evt)=>this.props.selectionUpdated(evt.target.value)} value={this.props.projectName}/></td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default CommissionTitleComponent;
