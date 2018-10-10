import React from 'react';
import PropTypes from 'prop-types';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';
import WorkingGroupSelector from '../common/WorkingGroupSelector.jsx';
import CommissionSelector from '../common/CommissionSelector.jsx';

class PlutoLinkageComponent extends CommonMultistepComponent {
    static propTypes = {
        valueWasSet: PropTypes.func.isRequired,
        currentPlutoCommission: PropTypes.number,
        currentWorkingGroup: PropTypes.number,
        workingGroupList: PropTypes.array.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            plutoCommissionRef: null,
            workingGroupRef: null,
            showStatus: "In production"
        }
    }

    componentWillMount(){
        this.setState({plutoCommissionRef: this.props.currentPlutoCommission, workingGroupRef: this.props.currentWorkingGroup})
    }

    render(){
        return <div>
            <h3>Select Commission</h3>
            <p>We need to know what piece of work this project file relates to.  Please select the relevant working group and commission</p>
            <table>
                <tbody>
                <tr>
                    <td>Working group</td>
                    <td><WorkingGroupSelector valueWasSet={value=>this.setState({workingGroupRef: value})}
                                          workingGroupList={this.props.workingGroupList}
                                          currentValue={this.state.workingGroupRef}/></td>
                </tr>
                <tr>
                    <td>Commission</td>
                    <td><CommissionSelector workingGroupId={this.state.workingGroupRef}
                                            selectedCommissionId={this.state.plutoCommissionRef}
                                            showStatus={this.state.showStatus}
                                            valueWasSet={value=>this.setState({plutoCommissionRef: value})}/>
                        <br />
                        <input id="only-show-production" type="radio" name="commission" checked={this.state.showStatus==="In production"} onChange={event=>this.setState({showStatus: "In production"})}/>
                        <label htmlFor="only-show-production" style={{display: "inline", marginLeft: "0.4em"}}>"In production"</label>
                        <input id="only-show-new" type="radio" name="commission" checked={this.state.showStatus==="New"} onChange={event=>this.setState({showStatus: "New"})}/>
                        <label htmlFor="only-show-new" style={{display: "inline", marginLeft: "0.4em"}}>"New"</label>
                        <input id="show-everything" type="radio" name="commission" checked={this.state.showStatus===null} onChange={event=>this.setState({showStatus: null})}/>
                        <label htmlFor="show-everything" style={{display: "inline", marginLeft: "0.4em"}}>Everything</label>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default PlutoLinkageComponent;